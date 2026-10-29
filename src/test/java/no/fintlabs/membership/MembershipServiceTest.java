package no.fintlabs.membership;

import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private MembershipService membershipService;

    @Test
    void shouldDeduplicateMembershipIdsAndAssignmentsByRole() {
        Membership firstMembership = membership("first", 10L);
        Membership secondMembership = membership("second", 10L);
        Assignment assignment = new Assignment();
        assignment.setRoleRef(10L);

        when(membershipRepository.findAllById(List.of("first", "second")))
                .thenReturn(List.of(firstMembership, secondMembership));
        when(assignmentService.getAssignmentsByRoles(List.of(10L))).thenReturn(List.of(assignment));

        membershipService.syncAssignmentsForMemberships(List.of("first", "second", "first"));

        verify(membershipRepository).findAllById(List.of("first", "second"));
        verify(assignmentService).getAssignmentsByRoles(List.of(10L));
        verify(flattenedAssignmentService).createOrUpdateFlattenedAssignmentsForMembership(assignment, firstMembership);
        verify(flattenedAssignmentService).createOrUpdateFlattenedAssignmentsForMembership(assignment, secondMembership);
    }

    @Test
    void shouldRejectUnknownMembershipIds() {
        when(membershipRepository.findAllById(List.of("known", "missing")))
                .thenReturn(List.of(membership("known", 10L)));

        assertThrows(ResourceNotFoundException.class,
                () -> membershipService.syncAssignmentsForMemberships(List.of("known", "missing")));

        verifyNoInteractions(assignmentService, flattenedAssignmentService);
    }

    private Membership membership(String id, Long roleId) {
        Membership membership = new Membership();
        membership.setId(id);
        membership.setRoleId(roleId);
        membership.setMemberId(20L);
        membership.setIdentityProviderUserObjectId(UUID.randomUUID());
        return membership;
    }
}
