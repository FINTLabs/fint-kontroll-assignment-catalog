package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FlattenedAssignmentServiceTest {

    @Mock
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @InjectMocks
    private FlattenedAssignmentService flattenedAssignmentService;

    @Test
    public void shouldCreateFlattenedAssignmentsWhenUserRefIsNotNull() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setUserRef(123L);

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAllAndFlush(any());
    }

    @Test
    public void shouldCreateFlattenedAssignmentsWhenRoleRefIsNotNullAndMembershipsNotEmpty() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);

        when(membershipRepository.findAll(isA(Specification.class))).thenReturn(Collections.singletonList(new Membership()));

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAllAndFlush(any());
    }

    @Test
    public void shouldNotCreateFlattenedAssignmentsWhenRoleRefIsNotNullAndMembershipsEmpty() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(456L);

        when(membershipRepository.findAll(isA(Specification.class))).thenReturn(Collections.emptyList());

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, times(0)).save(any());
    }

    @Test
    public void shouldNotCreateFlattenedAssignmentsWhenBothUserRefAndRoleRefAreNull() {
        Assignment assignment = new Assignment();

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, never()).save(any());
    }
}
