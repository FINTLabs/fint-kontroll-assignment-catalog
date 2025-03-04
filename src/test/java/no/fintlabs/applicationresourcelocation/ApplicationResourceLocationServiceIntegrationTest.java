package no.fintlabs.applicationresourcelocation;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.orgunitdistance.OrgUnitDistance;
import no.fintlabs.orgunitdistance.OrgUnitDistanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DataJpaTest
@Testcontainers
@Import({ApplicationResourceLocationService.class, OrgUnitDistanceService.class})
public class ApplicationResourceLocationServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private ApplicationResourceLocationService applicationResourceLocationService;

    @Autowired
    private OrgUnitDistanceService orgUnitDistanceService;

    @Autowired
    private ApplicationResourceLocationRepository applicationResourceLocationRepository;

    @Test
    public void shouldFindApplicationResourceLocation() {
        ApplicationResourceLocation applicationResourceLocation = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(100L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(50L)
                .build();
        applicationResourceLocationService.save(applicationResourceLocation);
        Optional<ApplicationResourceLocation> applicationResourceLocationOptional =
                applicationResourceLocationRepository.findById(applicationResourceLocation.getId());

        assertThat(applicationResourceLocationOptional.get().getApplicationResourceId()).isEqualTo(applicationResourceLocation.getApplicationResourceId());
    }

    @Test
    public void shouldUpdateApplicationResourceLocation() {
        ApplicationResourceLocation applicationResourceLocationOriginal = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(100L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(50L)
                .build();
        applicationResourceLocationService.save(applicationResourceLocationOriginal);

        ApplicationResourceLocation applicationResourceLocationUpdated = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(100L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(250L)
                .build();
        applicationResourceLocationService.save(applicationResourceLocationUpdated);

        Optional<ApplicationResourceLocation> applicationResourceLocationFromDB =
                applicationResourceLocationRepository.findById(applicationResourceLocationOriginal.getId());

        assertThat(applicationResourceLocationFromDB.get().getResourceLimit()).isEqualTo(250L);

        Long TotalRecords = applicationResourceLocationRepository.count();

        assertThat(TotalRecords).isEqualTo(1L);

    }
    @Test
    public void shouldFindNearestApplicationResourceLocationForOrgUnit() {
        ApplicationResourceLocation applicationResourceLocation = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(100L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(50L)
                .build();
        applicationResourceLocationService.save(applicationResourceLocation);

        OrgUnitDistance orgUnitDistance = OrgUnitDistance
                .builder()
                .id("org1_org1")
                .orgUnitId("org1")
                .subOrgUnitId("org1")
                .distance(0)
                .build();
        orgUnitDistanceService.save(orgUnitDistance);

        OrgUnitDistance orgUnitDistance1 = OrgUnitDistance
                .builder()
                .id("org1_org2")
                .orgUnitId("org1")
                .subOrgUnitId("org2")
                .distance(2)
                .build();
        orgUnitDistanceService.save(orgUnitDistance1);

        OrgUnitDistance orgUnitDistance2 = OrgUnitDistance
                .builder()
                .id("org2_org2")
                .orgUnitId("org2")
                .subOrgUnitId("org2")
                .distance(0)
                .build();
        orgUnitDistanceService.save(orgUnitDistance2);

        NearestResourceLocationDto nearestApplicationResourceLocation = applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(100L, "org1").get();
        assertThat(nearestApplicationResourceLocation.orgUnitId()).isEqualTo("org1");
        assertThat(nearestApplicationResourceLocation.orgUnitName()).isEqualTo("OrgUnit no 1");

        NearestResourceLocationDto nearestApplicationResourceLocation1 = applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(100L, "org2").get();
        assertThat(nearestApplicationResourceLocation1.orgUnitId()).isEqualTo("org1");
        assertThat(nearestApplicationResourceLocation1.orgUnitName()).isEqualTo("OrgUnit no 1");

        ApplicationResourceLocation applicationResourceLocation1 = ApplicationResourceLocation
                .builder()
                .id(2L)
                .applicationResourceId(100L)
                .resourceId("app1")
                .orgUnitId("org2")
                .orgUnitName("OrgUnit no 2")
                .resourceLimit(10L)
                .build();
        applicationResourceLocationService.save(applicationResourceLocation1);

        NearestResourceLocationDto nearestApplicationResourceLocation2 = applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(100L, "org2").get();
        assertThat(nearestApplicationResourceLocation2.orgUnitId()).isEqualTo("org2");
        assertThat(nearestApplicationResourceLocation2.orgUnitName()).isEqualTo("OrgUnit no 2");
    }
}
