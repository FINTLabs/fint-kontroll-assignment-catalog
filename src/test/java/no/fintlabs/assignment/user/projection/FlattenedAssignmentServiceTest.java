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
    void shouldRepublishActiveUserMembershipsInBatches() {
        List<UserEntraMembership> firstBatch = new ArrayList<>();
        for (long id = 1; id <= 500; id++) {
            firstBatch.add(UserEntraMembership.builder()
                    .id(id)
                    .userEntraId(UUID.randomUUID())
                    .resourceEntraId(UUID.randomUUID())
                    .membershipStatus(MembershipStatus.ACTIVE)
                    .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                    .build());
        }
        UserEntraMembership third = UserEntraMembership.builder()
                .id(501L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();

        when(userEntraMembershipRepository.findActiveMembershipsByIdGreaterThanOrderByIdAsc(eq(0L), any()))
                .thenReturn(firstBatch);
        when(userEntraMembershipRepository.findActiveMembershipsByIdGreaterThanOrderByIdAsc(eq(500L), any()))
                .thenReturn(List.of(third));

        flattenedAssignmentService.republishAllActiveUserMemberships();

        verify(assigmentEntityProducerService, times(501)).rePublish(any(UserEntraMembership.class));
        verifyNoMoreInteractions(assigmentEntityProducerService);
        verify(userEntraMembershipRepository).findActiveMembershipsByIdGreaterThanOrderByIdAsc(eq(0L), any());
        verify(userEntraMembershipRepository).findActiveMembershipsByIdGreaterThanOrderByIdAsc(eq(500L), any());
    }

    @Test
    void shouldRepublishUnconfirmedUserMembershipsInBatchesWithoutErrors() {
        UserEntraMembership activeMembership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
        UserEntraMembership inactiveMembership = UserEntraMembership.builder()
                .id(2L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.INACTIVE)
                .entraStatus(EntraMembershipSyncStatus.DELETION_SENT)
                .build();

        when(userEntraMembershipRepository.findByEntraStatusInAndIdGreaterThanOrderByIdAsc(anyList(), eq(0L), any()))
                .thenReturn(List.of(activeMembership, inactiveMembership));

        flattenedAssignmentService.republishUnconfirmedUserEntraMemberships();

        ArgumentCaptor<List<EntraMembershipSyncStatus>> statuses = ArgumentCaptor.forClass(List.class);
        verify(userEntraMembershipRepository).findByEntraStatusInAndIdGreaterThanOrderByIdAsc(statuses.capture(), eq(0L), any());
        assertEquals(List.of(
                EntraMembershipSyncStatus.NOT_SENT,
                EntraMembershipSyncStatus.SENT,
                EntraMembershipSyncStatus.TO_BE_DELETED,
                EntraMembershipSyncStatus.DELETION_SENT,
                EntraMembershipSyncStatus.NEEDS_REPUBLISH
        ), statuses.getValue());
        verify(assigmentEntityProducerService).publish(activeMembership, false);
        verify(assigmentEntityProducerService).publish(inactiveMembership, false);
        verifyNoMoreInteractions(assigmentEntityProducerService);
    }

    @Test
    void shouldRepublishUnconfirmedMembershipsForAssignmentInBatchesWithoutErrors() {
        UserEntraMembership activeMembership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
        UserEntraMembership inactiveMembership = UserEntraMembership.builder()
                .id(2L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.INACTIVE)
                .entraStatus(EntraMembershipSyncStatus.DELETION_SENT)
                .build();

        when(userEntraMembershipRepository.findByAssignmentIdAndEntraStatusInAndIdGreaterThanOrderByIdAsc(eq(10L), anyList(), eq(0L), any()))
                .thenReturn(List.of(activeMembership, inactiveMembership));

        flattenedAssignmentService.republishUnconfirmedUserEntraMembershipsByAssignmentId(10L);

        verify(assigmentEntityProducerService).publish(activeMembership, false);
        verify(assigmentEntityProducerService).publish(inactiveMembership, false);
        verifyNoMoreInteractions(assigmentEntityProducerService);
    }

    @Test
    void shouldRepublishEachMembershipOnceForSelectedFlattenedAssignmentsRegardlessOfStatus() {
        UserEntraMembership erroredMembership = UserEntraMembership.builder()
                .id(1L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.ERROR)
                .build();
        UserEntraMembership pendingDeletionMembership = UserEntraMembership.builder()
                .id(2L)
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.INACTIVE)
                .entraStatus(EntraMembershipSyncStatus.DELETION_SENT)
                .build();
        FlattenedAssignment first = flattenedAssignment(10L, null, UUID.randomUUID());
        first.setUserEntraMembership(erroredMembership);
        FlattenedAssignment duplicate = flattenedAssignment(11L, null, UUID.randomUUID());
        duplicate.setUserEntraMembership(erroredMembership);
        FlattenedAssignment terminated = flattenedAssignment(12L, new Date(), UUID.randomUUID());
        terminated.setUserEntraMembership(pendingDeletionMembership);

        when(flattenedAssignmentRepository.findAllById(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(first, duplicate, terminated));

        flattenedAssignmentService.republishSelectedFlattenedAssignments(List.of(10L, 11L, 12L));

        verify(assigmentEntityProducerService).publish(erroredMembership, true);
        verify(assigmentEntityProducerService).publish(pendingDeletionMembership, true);
        verifyNoMoreInteractions(assigmentEntityProducerService);
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
        assignment.setEntraGroupId(UUID.randomUUID());

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

        assertEquals(updatedUserEntraId, flattenedAssignment.getIdentityProviderUserObjectId());
        assertEquals(updatedUserEntraId, membership.getUserEntraId());
        verify(assigmentEntityProducerService).publishDeletion(resourceEntraId, previousUserEntraId);
        verify(assigmentEntityProducerService).publish(flattenedAssignment, true);
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

        flattenedAssignmentService.syncFlattenedAssignments(assignment, false);

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

        flattenedAssignmentService.syncFlattenedAssignments(assignment, false);

        verify(flattenedAssignmentRepository, times(1)).saveAll(any());
        verify(flattenedAssignmentRepository, times(1)).flush();
    }

    @Test
    public void shouldPublishDeletionWhenNoOtherActiveFlattenedAssignmentsExist() {

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setId(1L);
        flattenedAssignment.setAssignmentViaRoleRef(1L);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);
        flattenedAssignment.setAssignmentTerminationDate(new Date());

        when(flattenedAssignmentRepository.countOtherActiveAssignmentsByUserRefAndResourceRef(1L, 2L, 3L))
                .thenReturn(0L);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(1)).publishDeletion(flattenedAssignment);
    }

    @Test
    public void shouldNotPublishDeletionWhenOtherActiveFlattenedAssignmentsExist() {

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setId(1L);
        flattenedAssignment.setAssignmentViaRoleRef(1L);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);
        flattenedAssignment.setAssignmentTerminationDate(new Date());

        when(flattenedAssignmentRepository.countOtherActiveAssignmentsByUserRefAndResourceRef(1L, 2L, 3L))
                .thenReturn(1L);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, times(0)).publishDeletion(flattenedAssignment);
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

        when(flattenedAssignmentRepository.countOtherActiveAssignmentsByUserRefAndResourceRef(1L, 2L, 3L))
                .thenReturn(0L);
        when(flattenedAssignmentRepository.countOtherActiveAssignmentsByUserRefAndResourceRef(2L, 2L, 3L))
                .thenReturn(0L);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(first, second));

        verify(assigmentEntityProducerService).publishDeletion(first);
        verifyNoMoreInteractions(assigmentEntityProducerService);
    }

    @Test
    public void shouldNotPublishDeletionForDirectAssignmentWhenRoleAssignmentStillExists() {

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setId(1L);
        flattenedAssignment.setAssignmentViaRoleRef(null);
        flattenedAssignment.setUserRef(2L);
        flattenedAssignment.setResourceRef(3L);
        flattenedAssignment.setAssignmentTerminationDate(new Date());

        when(flattenedAssignmentRepository.countOtherActiveAssignmentsByUserRefAndResourceRef(1L, 2L, 3L))
                .thenReturn(1L);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment));

        verify(assigmentEntityProducerService, never()).publishDeletion(flattenedAssignment);
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

        flattenedAssignmentService.createOrUpdateFlattenedAssignmentsForMembership(a, m);

        verify(flattenedAssignmentRepository).saveAndFlush(any());
    }

}
