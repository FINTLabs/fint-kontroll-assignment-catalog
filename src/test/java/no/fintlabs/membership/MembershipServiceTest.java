package no.fintlabs.membership;

import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;

    @Mock
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void shouldDoNothingWhenNoAssignmentsExistForMembershipRole() {
        Membership membership = membership("role_1_user_10", 1L, 10L, "ACTIVE");

        when(assignmentService.getAssignmentsByRole(1L)).thenReturn(emptyList());

        membershipService.syncAssignmentsForMembership(membership);

        verify(assignmentService).getAssignmentsByRole(1L);
        verifyNoInteractions(flattenedAssignmentService);
        verifyNoInteractions(flattenedAssignmentMembershipService);
    }

    @Test
    void shouldSyncAssignmentsForEachAssignmentOnRole() {
        Membership membership = membership("role_1_user_10", 1L, 10L, "ACTIVE");

        Assignment assignment1 = assignment(100L, 1L);
        Assignment assignment2 = assignment(200L, 1L);

        FlattenedAssignment flattened1 = new FlattenedAssignment();
        flattened1.setAssignmentId(100L);
        flattened1.setUserRef(10L);
        flattened1.setAssignmentViaRoleRef(1L);

        FlattenedAssignment flattened2 = new FlattenedAssignment();
        flattened2.setAssignmentId(200L);
        flattened2.setUserRef(10L);
        flattened2.setAssignmentViaRoleRef(1L);

        when(assignmentService.getAssignmentsByRole(1L)).thenReturn(List.of(assignment1, assignment2));

        when(flattenedAssignmentService
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(100L, 10L, 1L))
                .thenReturn(List.of(flattened1));

        when(flattenedAssignmentService
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(200L, 10L, 1L))
                .thenReturn(List.of(flattened2));

        membershipService.syncAssignmentsForMembership(membership);

        verify(assignmentService).getAssignmentsByRole(1L);

        verify(flattenedAssignmentService)
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(100L, 10L, 1L);
        verify(flattenedAssignmentService)
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(200L, 10L, 1L);

        verify(flattenedAssignmentMembershipService)
                .createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment1, List.of(flattened1));
        verify(flattenedAssignmentMembershipService)
                .createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment2, List.of(flattened2));
    }

    @Test
    void shouldContinueProcessingWhenOneAssignmentFails() {
        Membership membership = membership("role_1_user_10", 1L, 10L, "ACTIVE");

        Assignment assignment1 = assignment(100L, 1L);
        Assignment assignment2 = assignment(200L, 1L);

        when(assignmentService.getAssignmentsByRole(1L)).thenReturn(List.of(assignment1, assignment2));

        when(flattenedAssignmentService
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(100L, 10L, 1L))
                .thenThrow(new RuntimeException("boom"));

        when(flattenedAssignmentService
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(200L, 10L, 1L))
                .thenReturn(List.of());

        membershipService.syncAssignmentsForMembership(membership);

        verify(flattenedAssignmentService)
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(100L, 10L, 1L);
        verify(flattenedAssignmentService)
                .getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(200L, 10L, 1L);

        verify(flattenedAssignmentMembershipService, never())
                .createOrUpdateFlattenedAssignmentsForExistingAssignment(eq(assignment1), any());

        verify(flattenedAssignmentMembershipService)
                .createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment2, List.of());
    }

    private Membership membership(String id, Long roleId, Long memberId, String status) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setRoleId(roleId);
        membership.setMemberId(memberId);
        membership.setMemberStatus(status);
        return membership;
    }

    private Assignment assignment(Long id, Long roleRef) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setRoleRef(roleRef);
        assignment.setId(id);
        assignment.setResourceRef(999L);
        return assignment;
    }
}