package no.fintlabs.applicationresourcelocation;

import no.fintlabs.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DataJpaTest
@Testcontainers
@Import({ApplicationResourceLocationService.class})
public class ApplicationResourceLocationServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private ApplicationResourceLocationService applicationResourceLocationService;

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
}
