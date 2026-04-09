package no.fintlabs.assignment.flattened;

import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;

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

    private Assignment assignment(long id) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setAssignmentId("A-" + id);
        assignment.setUserRef(10L);
        assignment.setAssignerRoleRef(20L);
        assignment.setAzureAdUserId(UUID.randomUUID());
        return assignment;
    }

    private Membership membership(long userRef, long roleRef, String status, UUID idp) {
        Membership membership = new Membership();
        membership.setMemberId(userRef);
        membership.setRoleId(roleRef);
        membership.setMemberStatus(status);
        membership.setIdentityProviderUserObjectId(idp);
        return membership;
    }

    private FlattenedAssignment flattenedAssignment(Long id, Date terminationDate, UUID idp) {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setId(id);
        flattenedAssignment.setAssignmentTerminationDate(terminationDate);
        flattenedAssignment.setIdentityProviderUserObjectId(idp);
        return flattenedAssignment;
    }
    @Test
    public void shouldCreateUserAssignment_manual() {
        Assignment assignment = assignment(1L);


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


    @Test
    void shouldDoNothing_whenInactiveMembershipAndNoExistingActiveFlattenedAssignments() {
        Assignment a = assignment(100L);
        Membership m = membership(10L, 20L,  "inactive", null);

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(
                        100L, 10L, 20L))
                .thenReturn(List.of());

        flattenedAssignmentService.createOrUpdateFlattenedAssignmentsForMembership(a, m);

        verify(flattenedAssignmentRepository, never()).saveAll(any());
        verify(flattenedAssignmentRepository, never()).flush();
        verifyNoInteractions(flattenedAssignmentMapper, assigmentEntityProducerService, flattenedAssignmentMembershipService);
    }

    @Test
    void shouldTerminateActiveFlattenedAssignments_whenMembershipBecomesInactive() {
        Assignment a = assignment(100L);
        Membership m = membership(10L, 20L, /*active*/ "inactive", UUID.randomUUID());

        FlattenedAssignment active1 = flattenedAssignment(1L, null,  UUID.randomUUID());
        FlattenedAssignment active2 = flattenedAssignment(2L, null,  UUID.randomUUID());

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(
                        100L, 10L, 20L))
                .thenReturn(List.of(active1, active2));

        ArgumentCaptor<Collection<FlattenedAssignment>> captor = ArgumentCaptor.forClass(Collection.class);

        long before = System.currentTimeMillis();
        flattenedAssignmentService.createOrUpdateFlattenedAssignmentsForMembership(a, m);
        long after = System.currentTimeMillis();

        verify(flattenedAssignmentRepository).saveAll(captor.capture());
        verify(flattenedAssignmentRepository).flush();

        Collection<FlattenedAssignment> saved = captor.getValue();
        Assertions.assertEquals(2, saved.size());
        for (FlattenedAssignment f : saved) {
            assertNotNull(f.getAssignmentTerminationDate(), "Termination date must be set");
            long ts = f.getAssignmentTerminationDate().getTime();
            assertTrue(ts >= before && ts <= after, "Termination timestamp should be 'now'");
        }

    }
    @Test
    void shouldCreateNewFlattenedAssignment_whenActiveMembershipAndNoExistingActiveFlattenedAssignments() {
        Assignment a = assignment(100L);
        Membership m = membership(10L, 20L, "ACTIVE", UUID.randomUUID());

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(
                        100L, 10L, 20L))
                .thenReturn(List.of());

        FlattenedAssignment mapped = toFlattenedAssignment(a);

        when(flattenedAssignmentRepository.saveAndFlush(any())).thenReturn(mapped);

        flattenedAssignmentService.createOrUpdateFlattenedAssignmentsForMembership(a, m);

        verify(flattenedAssignmentRepository).saveAndFlush(any());
    }

}
