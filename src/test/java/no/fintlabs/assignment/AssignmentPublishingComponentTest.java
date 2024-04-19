package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssignmentPublishingComponentTest {
    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;
    @Mock
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @InjectMocks
    private AssignmentPublishingComponent assignmentPublishingComponent;

    @Test
    public void shouldPublishUnConfirmedMembers() {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setIdentityProviderGroupMembershipConfirmed(false);

        when(flattenedAssignmentService.getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed()).thenReturn(List.of(flattenedAssignment));

        assignmentPublishingComponent.publishFlattenedAssignmentsUnConfirmed();

        verify(flattenedAssignmentService, times(1)).getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed();
        verify(assigmentEntityProducerService, times(1)).publish(flattenedAssignment);
    }

    @Test
    public void shouldNotPublishWhenConfirmedMembers() {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setIdentityProviderGroupMembershipConfirmed(true);

        when(flattenedAssignmentService.getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed()).thenReturn(List.of());

        assignmentPublishingComponent.publishFlattenedAssignmentsUnConfirmed();

        verify(flattenedAssignmentService, times(1)).getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed();
        verify(assigmentEntityProducerService, times(0)).publish(flattenedAssignment);
    }

    @Test
    public void shouldPublishDeletedAssignments_whenExist() {
        FlattenedAssignment deletedAssignment = new FlattenedAssignment();
        deletedAssignment.setAssignmentTerminationDate(new Date());

        when(flattenedAssignmentService.getFlattenedAssignmentsDeletedNotConfirmed()).thenReturn(List.of(deletedAssignment));

        assignmentPublishingComponent.publishDeletedFlattenedAssignments();

        verify(flattenedAssignmentService, times(1)).getFlattenedAssignmentsDeletedNotConfirmed();
        verify(assigmentEntityProducerService, times(1)).publishDeletion(deletedAssignment);
    }

    @Test
    public void shouldNotPublishDeletedAssignments_whenNoneExist() {
        when(flattenedAssignmentService.getFlattenedAssignmentsDeletedNotConfirmed()).thenReturn(List.of());

        assignmentPublishingComponent.publishDeletedFlattenedAssignments();

        verify(flattenedAssignmentService, times(1)).getFlattenedAssignmentsDeletedNotConfirmed();
        verify(assigmentEntityProducerService, times(0)).publishDeletion(any());
    }
}
