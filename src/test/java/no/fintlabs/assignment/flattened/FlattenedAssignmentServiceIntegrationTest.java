package no.fintlabs.assignment.flattened;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import no.fintlabs.opa.OpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
@Import({FlattenedAssignmentService.class,
        FlattenedAssignmentMapper.class
})
public class FlattenedAssignmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;
    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @MockBean
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;
//    @MockBean
//    private OpaService opaService;


    private FlattenedAssignment flattenedAssignment1, flattenedAssignment2, flattenedAssignment3,
            savedFlattenedAssignment1, savedFlattenedAssignment2, savedFlattenedAssignment3;


    @BeforeEach
    public void setUp() {

        flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setAssignmentId(1L);
        flattenedAssignment1.setResourceRef(10L);
        flattenedAssignment1.setAssignmentViaRoleRef(2L);
        flattenedAssignment1.setAssignmentTerminationDate(new Date());

        flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setResourceRef(10L);
        flattenedAssignment2.setAssignmentViaRoleRef(2L);

        flattenedAssignment3 = new FlattenedAssignment();
        flattenedAssignment3.setUserRef(10L);
        flattenedAssignment3.setAssignmentId(2L);
        flattenedAssignment3.setResourceRef(10L);
        flattenedAssignment3.setAssignmentViaRoleRef(3L);

        savedFlattenedAssignment1 = flattenedAssignmentRepository.save(flattenedAssignment1);
        savedFlattenedAssignment2 = flattenedAssignmentRepository.save(flattenedAssignment2);
        savedFlattenedAssignment3 = flattenedAssignmentRepository.save(flattenedAssignment3);
    }

    @Test
    public void testShouldPublishDeactivatedFlattenedAssignmentsForDeletion_withNoOtherActiveAssignments_shouldSetDeletionConfirmed() {

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment1));

        FlattenedAssignment deactivatedFlattenedAssignment = flattenedAssignmentRepository.findById(savedFlattenedAssignment1.getId()).orElseThrow();
        FlattenedAssignment activeFlattenedAssignment = flattenedAssignmentRepository.findById(savedFlattenedAssignment3.getId()).orElseThrow();

        assertThat(deactivatedFlattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed()).isTrue();
        assertThat(activeFlattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed()).isFalse();
    }
}

