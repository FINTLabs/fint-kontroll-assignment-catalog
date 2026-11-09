package no.fintlabs.assignment.user.entra;

import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEntraMembershipRepublisherTest {

    @Mock
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;
    @Mock
    private UserEntraMembershipPublisher assigmentEntityProducerService;
    private UserEntraMembershipRepublisher membershipRepublisher;

    @BeforeEach
    void setUp() {
        membershipRepublisher = new UserEntraMembershipRepublisher(
                userEntraMembershipRepository, flattenedAssignmentRepository, assigmentEntityProducerService, 72);
    }

    private FlattenedAssignment flattenedAssignment(Long id, Date terminationDate, UUID idp) {
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setId(id);
        assignment.setAssignmentTerminationDate(terminationDate);
        assignment.setIdentityProviderUserObjectId(idp);
        return assignment;
    }

    @Test
    void scheduledRepublishingUsesTheConfiguredResponseTimeoutAndOneDayFailureDelay() {
        long before = System.currentTimeMillis();

        membershipRepublisher.republishStaleMemberships();

        verify(userEntraMembershipRepository).findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                anyList(), any(), any(), any(), any(), any(),
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before - 72L * 60 * 60 * 1000)),
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before - 24L * 60 * 60 * 1000)),
                eq(0L), any());
    }

    private boolean isWithinOneSecondOf(Date value, long expectedMillis) {
        return Math.abs(value.getTime() - expectedMillis) < 1_000;
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

        membershipRepublisher.resendAddCommandsForActiveAssignments();

        verify(assigmentEntityProducerService).republishBatch(firstBatch);
        verify(assigmentEntityProducerService).republishBatch(List.of(third));
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

        membershipRepublisher.retryUnconfirmedMemberships();

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
    void shouldRepublishOnlyStaleUserMembershipsInBatches() {
        Date responseCutoff = new Date();
        Date failedCutoff = new Date(responseCutoff.getTime() + 48L * 60 * 60 * 1000);
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

        when(userEntraMembershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                anyList(), eq(MembershipStatus.ACTIVE), any(), eq(MembershipStatus.INACTIVE), any(), any(), eq(responseCutoff), eq(failedCutoff), eq(0L), any()))
                .thenReturn(List.of(activeMembership, inactiveMembership));

        membershipRepublisher.retryStaleMemberships(responseCutoff, failedCutoff);

        verify(userEntraMembershipRepository).findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                eq(List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED)),
                eq(MembershipStatus.ACTIVE),
                eq(EntraMembershipSyncStatus.SENT),
                eq(MembershipStatus.INACTIVE),
                eq(EntraMembershipSyncStatus.DELETION_SENT),
                eq(EntraMembershipSyncStatus.NEEDS_REPUBLISH),
                eq(responseCutoff), eq(failedCutoff), eq(0L), any());
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

        membershipRepublisher.retryUnconfirmedMembershipsForAssignment(10L);

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

        membershipRepublisher.forceRepublishMembershipsForFlattenedAssignments(List.of(10L, 11L, 12L));

        verify(assigmentEntityProducerService).publish(erroredMembership, true);
        verify(assigmentEntityProducerService).publish(pendingDeletionMembership, true);
        verifyNoMoreInteractions(assigmentEntityProducerService);
    }
}
