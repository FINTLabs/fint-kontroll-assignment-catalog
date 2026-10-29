package no.fintlabs.assignment.user.entra;

import no.fintlabs.entra.membership.EntraReturnCode;
import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.entra.membership.MembershipStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EntraUserGroupMembershipConsumerTest {

    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;

    @Mock
    private UserEntraMembershipPublisher assigmentEntityProducerService;

    @Mock
    private KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    private EntraUserGroupMembershipConsumer consumer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new EntraUserGroupMembershipConsumer(userEntraMembershipRepository, assigmentEntityProducerService, kafkaConsumerConfigurationDefaults);
    }

    @Test
    public void processGroupMembership_handlesDeletionConfirmation() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.REMOVED, groupId, userId)));

        assertEquals(no.fintlabs.entra.membership.EntraMembershipSyncStatus.DELETION_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_handlesUpdateConfirmation() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.ADDED, groupId, userId)));

        assertEquals(no.fintlabs.entra.membership.EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_republishesAdditionWhenRemovalConflictsWithActiveMembership() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.REMOVED, groupId, userId)));

        verify(assigmentEntityProducerService).publish(userEntraMembership, true);
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void processGroupMembership_republishesDeletionWhenAdditionConflictsWithInactiveMembership() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.ADDED, groupId, userId)));

        verify(assigmentEntityProducerService).publish(userEntraMembership, true);
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForActiveMembershipAsConfirmed() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.NO_CHANGES, groupId, userId)));

        assertEquals(no.fintlabs.entra.membership.EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForInactiveMembershipAsDeletionConfirmed() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.NO_CHANGES, groupId, userId)));

        assertEquals(no.fintlabs.entra.membership.EntraMembershipSyncStatus.DELETION_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_marksFailedAsNeedsRepublish() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.membership.EntraMembershipSyncStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.FAILED, groupId, userId)));

        assertEquals(no.fintlabs.entra.membership.EntraMembershipSyncStatus.NEEDS_REPUBLISH, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_doesNotSaveWhenMembershipIsMissing() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.empty());

        consumer.processGroupMembership(record(new EntraUserGroupMembershipResult(EntraReturnCode.ADDED, groupId, userId)));

        verify(userEntraMembershipRepository, times(1)).findByUserEntraIdAndResourceEntraId(userId, groupId);
        verify(userEntraMembershipRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(assigmentEntityProducerService);
    }

    private ConsumerRecord<String, EntraUserGroupMembershipResult> record(EntraUserGroupMembershipResult membership) {
        return new ConsumerRecord<>("topic", 1, 1, membership.getEntraGroupRef() + "_" + membership.getEntraUserRef(), membership);
    }

    private UserEntraMembership membership(
            UUID userId,
            UUID groupId,
            MembershipStatus membershipStatus,
            no.fintlabs.entra.membership.EntraMembershipSyncStatus entraStatus
    ) {
        return UserEntraMembership.builder()
                .userEntraId(userId)
                .resourceEntraId(groupId)
                .membershipStatus(membershipStatus)
                .entraStatus(entraStatus)
                .build();
    }
}
