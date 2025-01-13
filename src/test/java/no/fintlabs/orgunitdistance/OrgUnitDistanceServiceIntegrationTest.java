package no.fintlabs.orgunitdistance;

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
@Import({OrgUnitDistanceService.class})
public class OrgUnitDistanceServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private OrgUnitDistanceService orgUnitDistanceService;

    @Autowired
    private OrgUnitDistanceRepository orgUnitDistanceRepository;

    @Test
    public void shouldFindOrgUnitDistance() {
        OrgUnitDistance orgUnitDistance = OrgUnitDistance
                .builder()
                .id("a_b")
                .orgUnitId("a")
                .subOrgUnitId("b")
                .distance(3)
                .build();
        orgUnitDistanceService.save(orgUnitDistance);

        Optional<OrgUnitDistance> orgUnitDistanceOptional = orgUnitDistanceRepository.findById(orgUnitDistance.getId());
        assertThat(orgUnitDistanceOptional.get().getId()).isEqualTo(orgUnitDistance.getId());

    }

    @Test
    public void shouldUpdateOrgUnitDistance() {
        OrgUnitDistance orgUnitDistanceOrginal = OrgUnitDistance
                .builder()
                .id("a_b")
                .orgUnitId("a")
                .subOrgUnitId("b")
                .distance(3)
                .build();
        orgUnitDistanceService.save(orgUnitDistanceOrginal);

        OrgUnitDistance orgUnitDistanceUpdated = OrgUnitDistance
                .builder()
                .id("a_b")
                .orgUnitId("a")
                .subOrgUnitId("b")
                .distance(5)
                .build();
        orgUnitDistanceService.save(orgUnitDistanceUpdated);

        Optional<OrgUnitDistance> orgUnitDistanceFromDB = orgUnitDistanceRepository.findById(orgUnitDistanceUpdated.getId());
        assertThat(orgUnitDistanceFromDB.get().getDistance()).isEqualTo(5);
        Long totalRecords = orgUnitDistanceRepository.count();
        assertThat(totalRecords).isEqualTo(1);
    }
}