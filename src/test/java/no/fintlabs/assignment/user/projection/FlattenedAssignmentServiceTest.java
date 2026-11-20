package no.fintlabs.assignment.user.projection;

import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.membership.Membership;
import no.fintlabs.user.User;
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
    private UserEntraMembershipRepository userEntraMembershipRepository;

    @Mock
    private UserEntraMembershipPublisher assigmentEntityProducerService;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private FlattenedAssignmentService flattenedAssignmentService;

    private Assignment assignment(long id) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setAssignmentId("A-" + id);
        assignment.setUserRef(10L);
        assignment.setEntraUserId(UUID.randomUUID());
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
    void shouldRecalculateResourcesInAscendingOrderWhenTerminatingMembershipAssignments() {
        FlattenedAssignment first = flattenedAssignment(1L, null, null);
        first.setResourceRef(20L);
        FlattenedAssignment second = flattenedAssignment(2L, null, null);
        second.setResourceRef(10L);
        FlattenedAssignment third = flattenedAssignment(3L, null, null);
        third.setResourceRef(20L);

        when(flattenedAssignmentRepository.findById(1L)).thenReturn(Optional.of(first));
        when(flattenedAssignmentRepository.findById(2L)).thenReturn(Optional.of(second));
        when(flattenedAssignmentRepository.findById(3L)).thenReturn(Optional.of(third));

        flattenedAssignmentService.terminateByIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));

        ArgumentCaptor<Long> resourceIds = ArgumentCaptor.forClass(Long.class);
        verify(licenseEnforcementService, times(2)).recalculateAssignedResourcesForResource(resourceIds.capture());
        assertEquals(List.of(10L, 20L), resourceIds.getAllValues());
    }

    @Test
    public void shouldCreateUserAssignment_manual() {
        Assignment assignment = assignment(1L);


        flattenedAssignmentService.createForNewAssignment(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldCreateGroupAssignment_manual() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);
        assignment.setEntraGroupId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();

        when(flattenedAssignmentMembershipService.createFlattenedAssignmentsForNewRoleAssignment(assignment)).thenReturn(List.of(flattenedAssignment));

        flattenedAssignmentService.createForNewAssignment(assignment);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldNotCreateFlattenedAssignmentsWhenBothUserRefAndRoleRefAreNull() {
        Assignment assignment = new Assignment();

        flattenedAssignmentService.createForNewAssignment(assignment);

        verify(flattenedAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldReuseOneNewMembershipForAssignmentsWithTheSameEntraIds() {
        UUID userEntraId = UUID.randomUUID();
        UUID resourceEntraId = UUID.randomUUID();
        FlattenedAssignment first = flattenedAssignment(1L, null, userEntraId);
        first.setIdentityProviderGroupObjectId(resourceEntraId);
        first.setResourceRef(1L);
        FlattenedAssignment second = flattenedAssignment(2L, null, userEntraId);
        second.setIdentityProviderGroupObjectId(resourceEntraId);
        second.setResourceRef(1L);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userEntraId, resourceEntraId))
                .thenReturn(Optional.empty());
        when(flattenedAssignmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        flattenedAssignmentService.saveAndPublishFlattenedAssignmentsBatch(List.of(first, second), true);

        assertSame(first.getUserEntraMembership(), second.getUserEntraMembership());
        verify(userEntraMembershipRepository).findByUserEntraIdAndResourceEntraId(userEntraId, resourceEntraId);
        verify(assigmentEntityProducerService).publish(first.getUserEntraMembership(), false);
    }

    @Test
    void shouldMigrateMembershipAndRepublishAssignmentsWhenUserEntraIdChanges() {
        UUID previousUserEntraId = UUID.randomUUID();
        UUID updatedUserEntraId = UUID.randomUUID();
        UUID resourceEntraId = UUID.randomUUID();
        UserEntraMembership membership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(previousUserEntraId)
                .resourceEntraId(resourceEntraId)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedAssignment flattenedAssignment = flattenedAssignment(1L, null, previousUserEntraId);
        flattenedAssignment.setUserRef(10L);
        flattenedAssignment.setResourceRef(42L);
        flattenedAssignment.setIdentityProviderGroupObjectId(resourceEntraId);
        flattenedAssignment.setUserEntraMembership(membership);
        User user = new User();
        user.setId(10L);
        user.setIdentityProviderUserObjectId(updatedUserEntraId);

        when(flattenedAssignmentRepository.findByUserRefAndAssignmentTerminationDateIsNull(10L))
                .thenReturn(List.of(flattenedAssignment));
        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(updatedUserEntraId, resourceEntraId))
                .thenReturn(Optional.empty());

        flattenedAssignmentService.updateAssignmentsOnUserChange(user);

        assertEquals(updatedUserEntraId, membership.getUserEntraId());
        verify(assigmentEntityProducerService).publish(membership, true);
        verify(licenseEnforcementService).recalculateAssignedResourcesForResource(42L);
    }

    @Test
    void shouldPublishSharedMembershipOnceWhenUserEntraIdChanges() {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UserEntraMembership membership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(oldId)
                .resourceEntraId(resourceId)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedAssignment first = flattenedAssignment(1L, null, oldId);
        FlattenedAssignment second = flattenedAssignment(2L, null, oldId);
        first.setUserEntraMembership(membership);
        second.setUserEntraMembership(membership);
        User user = new User();
        user.setId(10L);
        user.setIdentityProviderUserObjectId(newId);

        when(flattenedAssignmentRepository.findByUserRefAndAssignmentTerminationDateIsNull(10L))
                .thenReturn(List.of(first, second));
        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(newId, resourceId))
                .thenReturn(Optional.empty());

        flattenedAssignmentService.updateAssignmentsOnUserChange(user);

        verify(assigmentEntityProducerService, times(1)).publish(membership, true);
    }

    @Test
    void shouldPublishMigratedMembershipOnceForRoleMembershipChange() {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UserEntraMembership entraMembership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(oldId)
                .resourceEntraId(resourceId)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedAssignment first = flattenedAssignment(1L, null, oldId);
        FlattenedAssignment second = flattenedAssignment(2L, null, oldId);
        first.setUserEntraMembership(entraMembership);
        second.setUserEntraMembership(entraMembership);
        Membership roleMembership = membership(10L, 20L, "ACTIVE", newId);

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(100L, 10L, 20L))
                .thenReturn(List.of(first, second));
        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(newId, resourceId))
                .thenReturn(Optional.empty());

        flattenedAssignmentService.reconcileForRoleMembership(assignment(100L), roleMembership);

        assertEquals(newId, entraMembership.getUserEntraId());
        verify(assigmentEntityProducerService, times(1)).publish(entraMembership, false);
    }

    @Test
    public void shouldSyncFlattenedRoleAssignments() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);
        assignment.setEntraGroupId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        when(flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments)).thenReturn(List.of(flattenedAssignment));//, true

        flattenedAssignmentService.reconcileForAssignmentAsync(assignment, false);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldSyncFlattenedUserAssignments() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setUserRef(123L);
        assignment.setEntraUserId(UUID.randomUUID());

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        List<FlattenedAssignment> existingAssignments = List.of(flattenedAssignment);

        when(flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId())).thenReturn(existingAssignments);
        when(flattenedAssignmentMapper.mapOriginWithExisting(any(), anyList())).thenReturn(Optional.of(flattenedAssignment));//, anyBoolean()

        flattenedAssignmentService.reconcileForAssignmentAsync(assignment, false);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldPublishDeletionWhenNoOtherActiveFlattenedAssignmentsExist() {
        UserEntraMembership membership = userEntraMembership(1L);
        FlattenedAssignment flattenedAssignment = terminatedFlattenedAssignment(1L, membership);

        when(flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, 1L))
                .thenReturn(false);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(1)).publishRemoval(flattenedAssignment);
    }

    @Test
    public void shouldNotPublishDeletionWhenOtherActiveFlattenedAssignmentsExist() {
        UserEntraMembership membership = userEntraMembership(1L);
        FlattenedAssignment flattenedAssignment = terminatedFlattenedAssignment(1L, membership);

        when(flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, 1L))
                .thenReturn(true);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(0)).publishRemoval(flattenedAssignment);
    }

    @Test
    void shouldPublishDeletionOnceWhenDeactivatedAssignmentsShareAMembership() {
        UserEntraMembership membership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();
        FlattenedAssignment first = flattenedAssignment(1L, new Date(), UUID.randomUUID());
        first.setUserRef(2L);
        first.setResourceRef(3L);
        first.setUserEntraMembership(membership);
        FlattenedAssignment second = flattenedAssignment(2L, new Date(), UUID.randomUUID());
        second.setUserRef(2L);
        second.setResourceRef(3L);
        second.setUserEntraMembership(membership);

        when(flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, 1L))
                .thenReturn(false);
        when(flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, 2L))
                .thenReturn(false);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(first, second));

        verify(assigmentEntityProducerService).publishRemoval(first);
        verifyNoMoreInteractions(assigmentEntityProducerService);
    }

    @Test
    public void shouldNotPublishDeletionForDirectAssignmentWhenRoleAssignmentStillExists() {

        UserEntraMembership membership = userEntraMembership(1L);
        FlattenedAssignment flattenedAssignment = terminatedFlattenedAssignment(1L, membership);
        flattenedAssignment.setAssignmentViaRoleRef(null);

        when(flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, 1L))
                .thenReturn(true);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, never()).publishRemoval(flattenedAssignment);
    }

    private UserEntraMembership userEntraMembership(Long id) {
        return UserEntraMembership.builder()
                .id(id)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();
    }

    private FlattenedAssignment terminatedFlattenedAssignment(Long id, UserEntraMembership membership) {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setId(id);
        flattenedAssignment.setAssignmentViaRoleRef(1L);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);
        flattenedAssignment.setAssignmentTerminationDate(new Date());
        flattenedAssignment.setUserEntraMembership(membership);
        return flattenedAssignment;
    }


    @Test
    void shouldDoNothing_whenInactiveMembershipAndNoExistingActiveFlattenedAssignments() {
        Assignment a = assignment(100L);
        Membership m = membership(10L, 20L,  "inactive", null);

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(
                        100L, 10L, 20L))
                .thenReturn(List.of());

        flattenedAssignmentService.reconcileForRoleMembership(a, m);

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
        UserEntraMembership sharedMembership = userEntraMembership(99L);
        active1.setUserEntraMembership(sharedMembership);
        active2.setUserEntraMembership(sharedMembership);

        when(flattenedAssignmentRepository
                .findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(
                        100L, 10L, 20L))
                .thenReturn(List.of(active1, active2));

        ArgumentCaptor<FlattenedAssignment> captor = ArgumentCaptor.forClass(FlattenedAssignment.class);

        long before = System.currentTimeMillis();
        flattenedAssignmentService.reconcileForRoleMembership(a, m);
        long after = System.currentTimeMillis();

        verify(flattenedAssignmentRepository, times(2)).saveAndFlush(captor.capture());
        verify(assigmentEntityProducerService, times(1)).publishRemoval(any(FlattenedAssignment.class));

        List<FlattenedAssignment> saved = captor.getAllValues();
        Assertions.assertEquals(2, saved.size());
        for (FlattenedAssignment f : saved) {
            assertNotNull(f.getAssignmentTerminationDate(), "Termination date must be set");
            assertEquals("Role membership deactivated", f.getAssignmentTerminationReason());
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

        flattenedAssignmentService.reconcileForRoleMembership(a, m);

        verify(flattenedAssignmentRepository).saveAndFlush(any());
    }

}
