package no.fintlabs.assignment.flattened;

import no.fintlabs.DatabaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
public class FlattenedAssignmentRepositoryIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    private FlattenedAssignment flattenedAssignment1, flattenedAssignment2, flattenedAssignment3,
                                savedFlattenedAssignment1, savedFlattenedAssignment2, savedFlattenedAssignment3;


    @BeforeEach
    public void setUp() {
        flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setAssignmentId(1L);
        flattenedAssignment1.setResourceRef(10L);
        flattenedAssignment1.setAssignmentViaRoleRef(2L);

        flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setAssignmentTerminationDate(new Date());
        flattenedAssignment2.setResourceRef(10L);
        flattenedAssignment2.setAssignmentViaRoleRef(2L);

        flattenedAssignment3 = new FlattenedAssignment();
        flattenedAssignment3.setUserRef(20L);
        flattenedAssignment3.setAssignmentId(2L);
        flattenedAssignment3.setResourceRef(10L);
        flattenedAssignment3.setAssignmentViaRoleRef(3L);

        savedFlattenedAssignment1 = flattenedAssignmentRepository.save(flattenedAssignment1);
        savedFlattenedAssignment2 = flattenedAssignmentRepository.save(flattenedAssignment2);
        savedFlattenedAssignment3 = flattenedAssignmentRepository.save(flattenedAssignment3);
    }

    @Test
    public void testFindByAssignmentIdAndAssignmentTerminationDateIsNull() {
        List<FlattenedAssignment> activeFlattenedAssignmentsForAssignment1L = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(1L);
        assertThat(activeFlattenedAssignmentsForAssignment1L.size()).isEqualTo(1);
        assertThat(activeFlattenedAssignmentsForAssignment1L.getFirst().getId()).isEqualTo(savedFlattenedAssignment1.getId());
    }

    @Test
    public void testfindByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull() {

        List<FlattenedAssignment> otherActiveFlattenedAssignments =
                flattenedAssignmentRepository.findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(
                        2L,
                        20L,
                        10L
        );

        assertThat(otherActiveFlattenedAssignments.size()).isEqualTo(1);
        assertThat(otherActiveFlattenedAssignments.getFirst().getId()).isEqualTo(savedFlattenedAssignment3.getId());
    }
    @Test
    public void testfindByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull_should_return_empty_list() {

        List<FlattenedAssignment> otherActiveFlattenedAssignments =
                flattenedAssignmentRepository.findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(
                        2L,
                        10L,
                        10L
                );

        assertThat(otherActiveFlattenedAssignments.size()).isEqualTo(0);
    }
}
