package no.fintlabs.enforcement;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Testcontainers
@Import({LicenseEnforcementService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LicenseEnforcementServiceConcurrencyTest extends DatabaseIntegrationTest {

    private static final long RESOURCE_ID = 10_001L;
    private static final String ORG_ID = "org1";

    @Autowired
    private LicenseEnforcementService service;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ApplicationResourceLocationRepository arlRepository;
    @MockBean
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    private Assignment assignmentToUserHardstop;

    @BeforeEach
    void setUp() {
        var field = ReflectionUtils.findField(LicenseEnforcementService.class, "hardstopEnabled");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, service, true);

        arlRepository.deleteAll();
        resourceRepository.deleteAll();

        Resource resource = Resource.builder()
                .id(RESOURCE_ID)
                .resourceId("app1")
                .resourceType("allTypes")
                .numberOfResourcesAssigned(0L)
                .resourceLimit(2L)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();
        resourceRepository.save(resource);

        ApplicationResourceLocation arl = ApplicationResourceLocation.builder()
                .id(1L)
                .applicationResourceId(RESOURCE_ID)
                .resourceId("app1")
                .orgUnitId(ORG_ID)
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(2L)
                .numberOfResourcesAssigned(0L)
                .build();
        arlRepository.save(arl);

        assignmentToUserHardstop = Assignment.builder()
                .id(111L)
                .assignerRef(222L)
                .userRef(333L)
                .resourceRef(RESOURCE_ID)
                .organizationUnitId(ORG_ID)
                .applicationResourceLocationOrgUnitId(ORG_ID)
                .azureAdUserId(UUID.randomUUID())
                .build();

        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    private List<Boolean> runConcurrently(int threads, Supplier<Boolean> action) {
        var executor = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);

        try {
            var futures = IntStream.range(0, threads)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try {
                            start.await();
                            return action.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        } catch (RuntimeException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, executor))
                    .toList();

            start.countDown();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    void concurrentIncrements_cappedAtTwo() {
        var results = runConcurrently(3, () -> service.updateAssignedLicense(assignmentToUserHardstop, 1L));
        long successes = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successes).isEqualTo(2);

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isEqualTo(2L);
        assertThat(arl.getNumberOfResourcesAssigned()).isEqualTo(2L);
    }

    @Test
    void concurrentIncrements_bothSucceedWithHighLimits() {
        var r0 = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        r0.setResourceLimit(2L);
        resourceRepository.save(r0);

        var arl0 = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        arl0.setResourceLimit(2L);
        arlRepository.save(arl0);

        var results = runConcurrently(2, () -> service.updateAssignedLicense(assignmentToUserHardstop, 1L));
        long successes = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successes).isEqualTo(2);

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isEqualTo(2L);
        assertThat(arl.getNumberOfResourcesAssigned()).isEqualTo(2L);
    }


    @Test
    void concurrentIncrements_cappedByARLLimit_onlyOneSucceeds() {
        var r0 = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        r0.setResourceLimit(10L);
        resourceRepository.save(r0);

        var arl0 = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        arl0.setResourceLimit(1L);
        arlRepository.save(arl0);

        var results = runConcurrently(3, () -> service.updateAssignedLicense(assignmentToUserHardstop, 1L));
        long successes = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successes).isEqualTo(1);

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isEqualTo(1L);
        assertThat(arl.getNumberOfResourcesAssigned()).isEqualTo(1L);
    }

    @Test
    void concurrentIncrements_cappedByResourceLimit_onlyOneSucceeds() {
        var r0 = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        r0.setResourceLimit(1L);
        resourceRepository.save(r0);

        var arl0 = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        arl0.setResourceLimit(10L);
        arlRepository.save(arl0);

        var results = runConcurrently(3, () -> service.updateAssignedLicense(assignmentToUserHardstop, 1L));
        long successes = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successes).isEqualTo(1);

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isEqualTo(1L);
        assertThat(arl.getNumberOfResourcesAssigned()).isEqualTo(1L);
    }

    @Test
    void updateAssignedLicense_missingARL_returnsFalse_noChanges() {
        Assignment bad = Assignment.builder()
                .resourceRef(RESOURCE_ID)
                .applicationResourceLocationOrgUnitId("unknown")
                .build();

        boolean ok = service.updateAssignedLicense(bad, 1L);
        assertThat(ok).isFalse();

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isZero();
        assertThat(arl.getNumberOfResourcesAssigned()).isZero();
        verify(resourceAvailabilityPublishingComponent, never())
                .updateResourceAvailability(Mockito.any(), Mockito.any());
    }

    @Test
    void zeroDelta_isNoop_andReturnsTrue() {
        boolean ok = service.updateAssignedLicense(assignmentToUserHardstop, 0L);
        assertThat(ok).isTrue();

        var r = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(r.getNumberOfResourcesAssigned()).isZero();
        assertThat(arl.getNumberOfResourcesAssigned()).isZero();
        verify(resourceAvailabilityPublishingComponent, never())
                .updateResourceAvailability(Mockito.any(), Mockito.any());
    }
}
