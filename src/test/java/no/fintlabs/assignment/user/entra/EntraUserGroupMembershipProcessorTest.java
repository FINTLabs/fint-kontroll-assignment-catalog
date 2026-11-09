package no.fintlabs.assignment.user.entra;

import no.fintlabs.assignment.entra.EntraMembershipBatchLookup;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.EntraReturnCode;
import no.fintlabs.entra.membership.MembershipStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntraUserGroupMembershipProcessorTest {

    @Mock private UserEntraMembershipRepository repository;
    @Mock private UserEntraMembershipPublisher publisher;
    @Mock private EntraMembershipBatchLookup batchLookup;
    @InjectMocks private EntraUserGroupMembershipProcessor processor;

    @Test
    void processesSeveralMembershipsWithOneLookupAndSave() {
        UserEntraMembership added = membership(MembershipStatus.ACTIVE);
        UserEntraMembership removed = membership(MembershipStatus.INACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(added, removed));

        processor.processBatch(List.of(
                record(0, added, EntraReturnCode.ADDED),
                record(1, removed, EntraReturnCode.REMOVED)));

        assertThat(added.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        assertThat(removed.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.DELETION_CONFIRMED);
        verify(batchLookup).findAll(argThat(keys -> keys.size() == 2), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository));
        verify(repository).saveAll(argThat(saved -> {
            List<UserEntraMembership> values = java.util.stream.StreamSupport
                    .stream(saved.spliterator(), false).toList();
            return values.size() == 2 && values.containsAll(List.of(added, removed));
        }));
        verifyNoInteractions(publisher);
    }

    @Test
    void appliesRepeatedResponsesForSameMembershipInOrder() {
        UserEntraMembership membership = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(membership));

        processor.processBatch(List.of(
                record(0, membership, EntraReturnCode.FAILED),
                record(1, membership, EntraReturnCode.ADDED)));

        assertThat(membership.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        verify(repository).saveAll(any());
    }

    @Test
    void republishesWhenResponseDisagreesWithDesiredStatus() {
        UserEntraMembership membership = membership(MembershipStatus.INACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(membership));

        processor.processBatch(List.of(record(0, membership, EntraReturnCode.ALREADY_PRESENT)));

        verify(publisher).publish(membership, true);
        verify(repository).saveAll(any());
    }

    @Test
    void republishesAdditionWhenRemovalConflictsWithActiveMembership() {
        UserEntraMembership membership = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository))).thenReturn(List.of(membership));

        processor.processBatch(List.of(record(0, membership, EntraReturnCode.REMOVED)));

        verify(publisher).publish(membership, true);
        verify(repository).saveAll(any());
    }

    @Test
    void confirmsAlreadyRemovedMembership() {
        UserEntraMembership membership = membership(MembershipStatus.INACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository))).thenReturn(List.of(membership));

        processor.processBatch(List.of(record(0, membership, EntraReturnCode.ALREADY_REMOVED)));

        assertThat(membership.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.DELETION_CONFIRMED);
        verifyNoInteractions(publisher);
    }

    @Test
    void mapsFailedAndErrorCodes() {
        UserEntraMembership failed = membership(MembershipStatus.ACTIVE);
        UserEntraMembership error = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(failed, error));

        processor.processBatch(List.of(
                record(0, failed, EntraReturnCode.FAILED),
                record(1, error, EntraReturnCode.ERROR)));

        assertThat(failed.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        assertThat(error.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.ERROR);
    }

    @Test
    void skipsInvalidAndUnknownMembershipsWithoutDroppingValidRecords() {
        UserEntraMembership known = membership(MembershipStatus.ACTIVE);
        UserEntraMembership unknown = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(known));

        processor.processBatch(List.of(
                new ConsumerRecord<>("topic", 0, 0L, "null", null),
                record(1, known, null),
                record(2, unknown, EntraReturnCode.ADDED),
                record(3, known, EntraReturnCode.ALREADY_PRESENT)));

        assertThat(known.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        verify(repository).saveAll(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void continuesAfterOneRecordFailsAndSavesTheOther() {
        UserEntraMembership bad = membership(MembershipStatus.INACTIVE);
        UserEntraMembership good = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(bad, good));
        doThrow(new IllegalStateException("publish failed")).when(publisher).publish(bad, true);

        processor.processBatch(List.of(
                record(0, bad, EntraReturnCode.ADDED),
                record(1, good, EntraReturnCode.ADDED)));

        assertThat(good.getEntraStatus()).isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        verify(repository).saveAll(argThat(saved -> {
            List<UserEntraMembership> values = java.util.stream.StreamSupport
                    .stream(saved.spliterator(), false).toList();
            return values.equals(List.of(good));
        }));
    }

    @Test
    void fallsBackToIndividualLookupWhenBatchLookupFails() {
        UserEntraMembership membership = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenThrow(new IllegalStateException("lookup failed"));
        when(repository.findByUserEntraIdAndResourceEntraId(
                membership.getUserEntraId(), membership.getResourceEntraId()))
                .thenReturn(Optional.of(membership));

        processor.processBatch(List.of(record(0, membership, EntraReturnCode.ADDED)));

        verify(repository).saveAll(any());
    }

    @Test
    void savesMembershipsIndividuallyIfBatchSaveFails() {
        UserEntraMembership bad = membership(MembershipStatus.ACTIVE);
        UserEntraMembership good = membership(MembershipStatus.ACTIVE);
        when(batchLookup.findAll(anySet(), eq(EntraMembershipBatchLookup.MembershipTable.USER), eq(repository)))
                .thenReturn(List.of(bad, good));
        when(repository.saveAll(any())).thenThrow(new IllegalStateException("batch save failed"));
        when(repository.save(bad)).thenThrow(new IllegalStateException("bad membership"));

        processor.processBatch(List.of(
                record(0, bad, EntraReturnCode.ADDED),
                record(1, good, EntraReturnCode.ADDED)));

        verify(repository).save(bad);
        verify(repository).save(good);
    }

    private UserEntraMembership membership(MembershipStatus status) {
        return UserEntraMembership.builder()
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(status)
                .build();
    }

    private ConsumerRecord<String, EntraUserGroupMembershipResult> record(
            long offset, UserEntraMembership membership, EntraReturnCode code) {
        EntraUserGroupMembershipResult payload = new EntraUserGroupMembershipResult(
                code, membership.getResourceEntraId(), membership.getUserEntraId());
        return new ConsumerRecord<>("topic", 0, offset, "key-" + offset, payload);
    }
}
