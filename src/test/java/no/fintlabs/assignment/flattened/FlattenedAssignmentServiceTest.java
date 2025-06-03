package no.fintlabs.assignment.flattened;

import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
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
        assignment.setAzureAdUserId(UUID.randomUUID());

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldCreateGroupAssignment_manual() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        //List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        //when(flattenedAssignmentRepository.findByAssignmentId(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMembershipService.createFlattenedAssignmentsForNewRoleAssignment(assignment)).thenReturn(List.of(flattenedAssignment));

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldNotCreateFlattenedAssignmentsWhenBothUserRefAndRoleRefAreNull() {
        Assignment assignment = new Assignment();

        flattenedAssignmentService.createFlattenedAssignments(assignment);

        verify(flattenedAssignmentRepository, never()).save(any());
    }

    @Test
    public void shouldSyncFlattenedRoleAssignments() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        when(flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments)).thenReturn(List.of(flattenedAssignment));//, true

        flattenedAssignmentService.syncFlattenedAssignments(assignment, true);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldSyncFlattenedUserAssignments() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setUserRef(123L);
        assignment.setAzureAdUserId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        when(flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMapper.mapOriginWithExisting(any(), anyList())).thenReturn(Optional.of(flattenedAssignment));//, anyBoolean()

        flattenedAssignmentService.syncFlattenedAssignments(assignment, true);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldPublishDeletionWhenNoOtherActiveFlattenedAssignmentsExist() {

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setAssignmentViaRoleRef(1L);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);

        when(flattenedAssignmentRepository.findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(1L, 2L, 3L))
                .thenReturn(new ArrayList<>());

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(1)).publishDeletion(flattenedAssignment);
    }

    @Test
    public void shouldNotPublishDeletionWhenOtherActiveFlattenedAssignmentsExist() {

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setAssignmentViaRoleRef(1L);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);

        when(flattenedAssignmentRepository.findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(1L, 2L, 3L))
                .thenReturn(List.of(new FlattenedAssignment()));

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(0)).publishDeletion(flattenedAssignment);
    }
}
