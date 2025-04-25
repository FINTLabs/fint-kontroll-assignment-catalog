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

    private FlattenedAssignment flattenedAssignment1, flattenedAssignment2, flattenedAssignment3;


    @BeforeEach
    public void setUp() {
        flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setId(1000L);
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setAssignmentId(1L);

        flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setId(1001L);
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setAssignmentTerminationDate(new Date());

        flattenedAssignment3 = new FlattenedAssignment();
        flattenedAssignment3.setId(1002L);
        flattenedAssignment3.setUserRef(30L);
        flattenedAssignment3.setAssignmentId(2L);

        flattenedAssignmentRepository.save(flattenedAssignment1);
        flattenedAssignmentRepository.save(flattenedAssignment2);
        flattenedAssignmentRepository.save(flattenedAssignment3);
    }

    @Test
    public void testFindByAssignmentIdAndAssignmentTerminationDateIsNull() {
        List<FlattenedAssignment> activeFlattenedAssignmentsForAssignment1L = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(1L);
        assertThat(activeFlattenedAssignmentsForAssignment1L.size()).isEqualTo(1);
        assertThat(activeFlattenedAssignmentsForAssignment1L.getFirst().getUserRef()).isEqualTo(10L);
    }
}
