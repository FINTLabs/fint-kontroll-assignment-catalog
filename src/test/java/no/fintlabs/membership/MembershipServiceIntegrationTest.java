package no.fintlabs.membership;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.user.UserLookupService;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@Testcontainers
@Import({
        MembershipService.class,
        FlattenedAssignmentService.class,
        FlattenedAssignmentMembershipService.class,
        FlattenedAssignmentMapper.class,
        UserLookupService.class
})
class MembershipServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLookupService userLookupService;

    @MockBean
    private AssignmentService assignmentService;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;

    private User user;
    private Membership membership;
    private Assignment assignment;
    private UUID identityProviderGroupObjectId;

    @BeforeEach
    void setUp() {
        flattenedAssignmentRepository.deleteAll();
        membershipRepository.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setId(10L);
        user.setIdentityProviderUserObjectId(UUID.randomUUID());
        userRepository.save(user);

        membership = new Membership();
        membership.setId("1_10");
        membership.setRoleId(1L);
        membership.setMemberId(10L);
        membership.setMemberStatus("ACTIVE");
        membershipRepository.save(membership);

        assignment = new Assignment();
        assignment.setId(100L);
        assignment.setRoleRef(1L);
        assignment.setId(100L);
        assignment.setResourceRef(500L);
        assignment.setAzureAdGroupId(identityProviderGroupObjectId);
    }

    @Test
    void shouldDoNothingWhenNoAssignmentsExistForRole() {
        org.mockito.Mockito.when(assignmentService.getAssignmentsByRole(1L)).thenReturn(List.of());

        membershipService.syncAssignmentsForMembership(membership);

        assertThat(flattenedAssignmentRepository.findAll()).isEmpty();
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    void shouldCreateFlattenedAssignmentForMatchingMembershipAndRoleAssignment() {
        org.mockito.Mockito.when(assignmentService.getAssignmentsByRole(1L)).thenReturn(List.of(assignment));

        membershipService.syncAssignmentsForMembership(membership);

        List<FlattenedAssignment> saved = flattenedAssignmentRepository.findAll();

        assertThat(saved).hasSize(1);

        FlattenedAssignment flattenedAssignment = saved.getFirst();
        assertThat(flattenedAssignment.getAssignmentId()).isEqualTo(100L);
        assertThat(flattenedAssignment.getAssignmentViaRoleRef()).isEqualTo(1L);
        assertThat(flattenedAssignment.getUserRef()).isEqualTo(10L);
        assertThat(flattenedAssignment.getIdentityProviderUserObjectId()).isEqualTo(user.getIdentityProviderUserObjectId());
        assertThat(flattenedAssignment.getAssignmentTerminationDate()).isNull();
    }

    @Test
    void shouldNotCreateDuplicateFlattenedAssignmentWhenExistingActiveAssignmentAlreadyMatches() {
        org.mockito.Mockito.when(assignmentService.getAssignmentsByRole(1L)).thenReturn(List.of(assignment));

        FlattenedAssignment existing = new FlattenedAssignment();
        existing.setAssignmentId(100L);
        existing.setAssignmentViaRoleRef(1L);
        existing.setUserRef(10L);
        existing.setResourceRef(500L);
        existing.setIdentityProviderUserObjectId(user.getIdentityProviderUserObjectId());
        existing.setIdentityProviderGroupObjectId(identityProviderGroupObjectId);
        flattenedAssignmentRepository.save(existing);

        membershipService.syncAssignmentsForMembership(membership);

        List<FlattenedAssignment> saved = flattenedAssignmentRepository.findAll();
        assertThat(saved).hasSize(1);
    }
}
