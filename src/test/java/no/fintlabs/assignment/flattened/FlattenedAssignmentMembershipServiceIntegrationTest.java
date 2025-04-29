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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
@Import({FlattenedAssignmentService.class,
        FlattenedAssignmentMapper.class,
        FlattenedAssignmentMembershipService.class
})
public class FlattenedAssignmentMembershipServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;
    @Autowired
    private MembershipRepository membershipRepository;
    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;
//    @MockBean
//    private OpaService opaService;

    private Membership activeMembership, inactiveMembership;
    private UUID activeUserObjectId, inactiveUserObjectId, groupObjectId;
    private Assignment assignment;
    private List<FlattenedAssignment> existingAssignments;

    @BeforeEach
    public void setUp() {

        activeUserObjectId = UUID.randomUUID();
        inactiveUserObjectId = UUID.randomUUID();
        groupObjectId = UUID.randomUUID();

        existingAssignments = new ArrayList<>();
        activeMembership = new Membership();
        activeMembership.setId("1_10");
        activeMembership.setRoleId(1L);
        activeMembership.setMemberId(10L);
        activeMembership.setIdentityProviderUserObjectId(activeUserObjectId);
        activeMembership.setMemberStatus("ACTIVE");

        inactiveMembership = new Membership();
        inactiveMembership.setId("1_20");
        inactiveMembership.setRoleId(1L);
        inactiveMembership.setMemberId(20L);
        inactiveMembership.setIdentityProviderUserObjectId(inactiveUserObjectId);
        inactiveMembership.setMemberStatus("INACTIVE");

        membershipRepository.save(activeMembership);
        membershipRepository.save(inactiveMembership);

        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setRoleRef(1L);
        assignment.setResourceRef(15L);
        assignment.setAzureAdGroupId(groupObjectId);


        FlattenedAssignment flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setId(1000L);
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setIdentityProviderUserObjectId(activeUserObjectId);
        flattenedAssignment1.setIdentityProviderGroupObjectId(groupObjectId);
        flattenedAssignment1.setAssignmentId(1L);
        flattenedAssignment1.setResourceRef(15L);
        flattenedAssignment1.setAssignmentViaRoleRef(1L);

        FlattenedAssignment flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setId(1001L);
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setIdentityProviderUserObjectId(inactiveUserObjectId);
        flattenedAssignment2.setIdentityProviderGroupObjectId(groupObjectId);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setResourceRef(15L);
        flattenedAssignment2.setAssignmentViaRoleRef(1L);

        existingAssignments.add(flattenedAssignment1);
        existingAssignments.add(flattenedAssignment2);
    }

    @Test
    public void testCreateFlattenedAssignmentsForNewRoleAssignment_withOneActiveMembership_shouldReturnOneFlattenedAssignment() {

        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentMembershipService.createFlattenedAssignmentsForNewRoleAssignment(assignment);

        assertThat(flattenedAssignments.size()).isEqualTo(1);
        assertThat(flattenedAssignments.getFirst().getUserRef()).isEqualTo(10L);
    }

    @Test
    public void testCreateOrUpdateFlattenedAssignmentsForExistingAssignmentWithIsSyncTrue_shouldReturnOneFlattenedAssignment() {

        List<FlattenedAssignment> flattenedAssignments =
                flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, true);

        assertThat(flattenedAssignments.size()).isEqualTo(1);
        assertThat(flattenedAssignments.getFirst().getUserRef()).isEqualTo(20L);
        assertThat(flattenedAssignments.getFirst().getAssignmentTerminationDate()).isNotNull();
    }

    @Test
    public void testCreateOrUpdateFlattenedAssignmentsForExistingAssignmentWithIsSyncFalse_shouldReturnOneFlattenedAssignment() {

        List<FlattenedAssignment> flattenedAssignments =
                flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, false);

        assertThat(flattenedAssignments.size()).isEqualTo(1);
        assertThat(flattenedAssignments.getFirst().getUserRef()).isEqualTo(20L);
        assertThat(flattenedAssignments.getFirst().getAssignmentTerminationDate()).isNotNull();
    }
}
