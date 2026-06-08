package no.fintlabs.enforcement;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
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
    private static final UUID RESOURCE_ENTRA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    private LicenseEnforcementService service;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ApplicationResourceLocationRepository arlRepository;
    @Autowired
    private UserEntraMembershipRepository userEntraMembershipRepository;
    @MockBean
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    private Assignment assignmentToUserHardstop;

    @BeforeEach
    void setUp() {
        var field = ReflectionUtils.findField(LicenseEnforcementService.class, "hardstopEnabled");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, service, true);

        userEntraMembershipRepository.deleteAll();
        arlRepository.deleteAll();
        resourceRepository.deleteAll();

        Resource resource = Resource.builder()
                .id(RESOURCE_ID)
                .resourceId("app1")
                .resourceType("allTypes")
                .identityProviderGroupObjectId(RESOURCE_ENTRA_ID)
                .numberOfResourcesAssigned(0L)
                .resourceLimit(10L)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();
        resourceRepository.save(resource);

        ApplicationResourceLocation arl = ApplicationResourceLocation.builder()
                .id(1L)
                .applicationResourceId(RESOURCE_ID)
                .resourceId("app1")
                .orgUnitId(ORG_ID)
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(10L)
                .numberOfResourcesAssigned(0L)
                .build();
        arlRepository.save(arl);

        assignmentToUserHardstop = Assignment.builder()
                .id(111L)
                .userRef(333L)
                .resourceRef(RESOURCE_ID)
                .organizationUnitId(ORG_ID)
                .applicationResourceLocationOrgUnitId(ORG_ID)
                .entraUserId(UUID.randomUUID())
                .entraGroupId(RESOURCE_ENTRA_ID)
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
    void concurrentRecalculationsPersistActiveMembershipCount() {
        saveActiveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        saveActiveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000002"));

        var results = runConcurrently(3, () -> service.recalculateAssignedResources(assignmentToUserHardstop));

        assertThat(results).containsOnly(true);
        var resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(2L);
        assertThat(arl.getNumberOfResourcesAssigned()).isZero();
    }

    @Test
    void hardstopResourceLimitUsesActiveMembershipCount() {
        var resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        resource.setResourceLimit(1L);
        resourceRepository.save(resource);
        saveActiveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        saveActiveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000002"));

        boolean ok = service.recalculateAssignedResources(assignmentToUserHardstop);

        assertThat(ok).isFalse();
        var unchanged = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        assertThat(unchanged.getNumberOfResourcesAssigned()).isZero();
    }

    @Test
    void updateAssignedLicense_ignoresAssignmentOrgUnitAndRecalculatesResourceLocations() {
        Assignment bad = Assignment.builder()
                .resourceRef(RESOURCE_ID)
                .applicationResourceLocationOrgUnitId("unknown")
                .build();

        boolean ok = service.recalculateAssignedResources(bad);

        assertThat(ok).isTrue();
        var resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        var arl = arlRepository.findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_ID).getFirst();
        assertThat(resource.getNumberOfResourcesAssigned()).isZero();
        assertThat(arl.getNumberOfResourcesAssigned()).isZero();
        verify(resourceAvailabilityPublishingComponent)
                .updateResourceAvailability(Mockito.any(), Mockito.any());
    }

    @Test
    void zeroDeltaStillRecalculatesFromActiveMemberships() {
        saveActiveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000001"));

        boolean ok = service.recalculateAssignedResources(assignmentToUserHardstop);

        assertThat(ok).isTrue();
        var resource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(1L);
    }

    private void saveActiveUserMembership(UUID userEntraId) {
        userEntraMembershipRepository.saveAndFlush(UserEntraMembership.builder()
                .userEntraId(userEntraId)
                .resourceEntraId(RESOURCE_ENTRA_ID)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraStatus.MEMBERSHIP_CONFIRMED)
                .build());
    }
}
