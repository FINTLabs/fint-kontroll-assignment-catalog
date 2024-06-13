package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;

    @Mock
    private FlattenedAssignmentMapper flattenedAssignmentMapper;

    @Mock
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @InjectMocks
    private FlattenedAssignmentService flattenedAssignmentService;

    @Test
    public void shouldCreateUserAssignment_manual() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setUserRef(123L);

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);
        when(flattenedAssignmentRepository.findByAssignmentId(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMapper.mapOriginWithExisting(isA(FlattenedAssignment.class), anyList(), isA(Boolean.class))).thenReturn(Optional.of(flattenedAssignment));

        flattenedAssignmentService.createFlattenedAssignments(assignment, false);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldCreateGroupAssignment_manual() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        when(flattenedAssignmentRepository.findByAssignmentId(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMembershipService.findMembershipsToCreateOrUpdate(assignment, existingAssignments, false)).thenReturn(List.of(flattenedAssignment));

        flattenedAssignmentService.createFlattenedAssignments(assignment, false);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldNotCreateFlattenedAssignmentsWhenBothUserRefAndRoleRefAreNull() {
        Assignment assignment = new Assignment();

        flattenedAssignmentService.createFlattenedAssignments(assignment, false);

        verify(flattenedAssignmentRepository, never()).save(any());
    }
}
