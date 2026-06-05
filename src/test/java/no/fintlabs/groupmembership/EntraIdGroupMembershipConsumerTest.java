package no.fintlabs.groupmembership;

import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.entra.MembershipStatus;
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

public class EntraIdGroupMembershipConsumerTest {

    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;

    @Mock
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @Mock
    private KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    private EntraIdGroupMembershipConsumer consumer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new EntraIdGroupMembershipConsumer(userEntraMembershipRepository, assigmentEntityProducerService, kafkaConsumerConfigurationDefaults);
    }

    @Test
    public void processGroupMembership_handlesDeletionConfirmation() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.EntraStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.REMOVED, groupId, userId)));

        assertEquals(no.fintlabs.entra.EntraStatus.DELETION_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_handlesUpdateConfirmation() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.EntraStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.ADDED, groupId, userId)));

        assertEquals(no.fintlabs.entra.EntraStatus.MEMBERSHIP_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_republishesAdditionWhenRemovalConflictsWithActiveMembership() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.EntraStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.REMOVED, groupId, userId)));

        verify(assigmentEntityProducerService).publish(userEntraMembership, true);
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void processGroupMembership_republishesDeletionWhenAdditionConflictsWithInactiveMembership() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.EntraStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.ADDED, groupId, userId)));

        verify(assigmentEntityProducerService).publish(userEntraMembership, true);
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForActiveMembershipAsConfirmed() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.EntraStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.NO_CHANGES, groupId, userId)));

        assertEquals(no.fintlabs.entra.EntraStatus.MEMBERSHIP_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForInactiveMembershipAsDeletionConfirmed() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.INACTIVE, no.fintlabs.entra.EntraStatus.DELETION_SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.NO_CHANGES, groupId, userId)));

        assertEquals(no.fintlabs.entra.EntraStatus.DELETION_CONFIRMED, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_marksFailedAsNeedsRepublish() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntraMembership userEntraMembership = membership(userId, groupId, MembershipStatus.ACTIVE, no.fintlabs.entra.EntraStatus.SENT);

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.of(userEntraMembership));

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.FAILED, groupId, userId)));

        assertEquals(no.fintlabs.entra.EntraStatus.NEEDS_REPUBLISH, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_doesNotSaveWhenMembershipIsMissing() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(userEntraMembershipRepository.findByUserEntraIdAndResourceEntraId(userId, groupId))
                .thenReturn(Optional.empty());

        consumer.processGroupMembership(record(new EntraIdGroupMembership(EntraStatus.ADDED, groupId, userId)));

        verify(userEntraMembershipRepository, times(1)).findByUserEntraIdAndResourceEntraId(userId, groupId);
        verify(userEntraMembershipRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(assigmentEntityProducerService);
    }

    private ConsumerRecord<String, EntraIdGroupMembership> record(EntraIdGroupMembership membership) {
        return new ConsumerRecord<>("topic", 1, 1, membership.getEntraGroupRef() + "_" + membership.getEntraUserRef(), membership);
    }

    private UserEntraMembership membership(
            UUID userId,
            UUID groupId,
            MembershipStatus membershipStatus,
            no.fintlabs.entra.EntraStatus entraStatus
    ) {
        return UserEntraMembership.builder()
                .userEntraId(userId)
                .resourceEntraId(groupId)
                .membershipStatus(membershipStatus)
                .entraStatus(entraStatus)
                .build();
    }
}
