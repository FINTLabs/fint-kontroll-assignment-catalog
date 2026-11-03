package no.fintlabs.assignment;

import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.entra.membership.event.OperationType;
import no.fintlabs.entra.membership.event.UserResourceGroupMembershipCommand;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssigmentEntityProducerServiceTest {
    @Mock
    private ParameterizedTemplateFactory entityProducerFactory;

    @Mock
    private EventTopicService eventTopicService;

    @Mock
    private ParameterizedTemplate entityProducer;

    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;

    private UserEntraMembershipPublisher service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(entityProducerFactory.createTemplate(any())).thenReturn(entityProducer);
        service = new UserEntraMembershipPublisher(entityProducerFactory, eventTopicService, userEntraMembershipRepository);
    }

    @Test
    public void shouldPublish_validAssignment() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setUserEntraMembership(UserEntraMembership.builder()
                .resourceEntraId(groupRef)
                .userEntraId(userRef)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build());

        service.publish(assignment);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.ADD, record.getValue().getOperation());
        assertEquals(groupRef, record.getValue().getEntraGroupRef());
        assertEquals(userRef, record.getValue().getEntraUserRef());
    }

    @Test
    public void shouldNotPublish_inValidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publish(assignment);

        verify(entityProducer, times(0)).send(any(ParameterizedProducerRecord.class));
    }

    @Test
    public void shouldPublishDeletion_validAssignment() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setUserEntraMembership(UserEntraMembership.builder()
                .resourceEntraId(groupRef)
                .userEntraId(userRef)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build());

        service.publishRemoval(assignment);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.REMOVE, record.getValue().getOperation());
        assertEquals(groupRef, record.getValue().getEntraGroupRef());
        assertEquals(userRef, record.getValue().getEntraUserRef());
    }

    @Test
    public void shouldNotPublishDeletion_invalidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publishRemoval(assignment);

        verify(entityProducer, times(0)).send(any(ParameterizedProducerRecord.class));
    }

    @Test
    public void shouldPublishAddFromUserEntraMembershipAndMarkAsSent() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        UserEntraMembership userEntraMembership = UserEntraMembership.builder()
                .resourceEntraId(groupRef)
                .userEntraId(userRef)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build();

        service.publish(userEntraMembership, false);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.ADD, record.getValue().getOperation());
        assertEquals(EntraMembershipSyncStatus.SENT, userEntraMembership.getEntraStatus());
        assertNotNull(userEntraMembership.getSentToEntraAt());
        assertNull(userEntraMembership.getDeletionSentToEntraAt());
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void shouldRepublishAddAndUpdateLastSentTimeWithoutChangingConfirmedStatus() {
        Date previousAdd = new Date(1_000L);
        Date previousRemoval = new Date(2_000L);
        UserEntraMembership membership = UserEntraMembership.builder()
                .resourceEntraId(UUID.randomUUID())
                .userEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .sentToEntraAt(previousAdd)
                .deletionSentToEntraAt(previousRemoval)
                .build();

        service.republish(membership);

        assertEquals(OperationType.ADD, captureSentRecord().getValue().getOperation());
        assertTrue(membership.getSentToEntraAt().after(previousAdd));
        assertEquals(previousRemoval, membership.getDeletionSentToEntraAt());
        assertEquals(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED, membership.getEntraStatus());
        verify(userEntraMembershipRepository).save(membership);
    }

    @Test
    public void shouldNotRecordRepublishWhenEntraIdsAreMissing() {
        UserEntraMembership membership = UserEntraMembership.builder()
                .userEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();

        service.republish(membership);

        assertNull(membership.getSentToEntraAt());
        verify(userEntraMembershipRepository, never()).save(any());
        verify(entityProducer, never()).send(any(ParameterizedProducerRecord.class));
    }

    @Test
    public void shouldDeferKafkaSendUntilTransactionCommit() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        UserEntraMembership userEntraMembership = UserEntraMembership.builder()
                .resourceEntraId(groupRef)
                .userEntraId(userRef)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build();

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.publish(userEntraMembership, false);

            assertEquals(EntraMembershipSyncStatus.SENT, userEntraMembership.getEntraStatus());
            verify(userEntraMembershipRepository).save(userEntraMembership);
            verify(entityProducer, never()).send(any(ParameterizedProducerRecord.class));

            List<TransactionSynchronization> synchronizations =
                    new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
            assertEquals(1, synchronizations.size());

            synchronizations.forEach(TransactionSynchronization::afterCommit);

            ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
            assertEquals(OperationType.ADD, record.getValue().getOperation());
            assertEquals(groupRef, record.getValue().getEntraGroupRef());
            assertEquals(userRef, record.getValue().getEntraUserRef());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void shouldSkipErrorMembershipUnlessForced() {
        UserEntraMembership userEntraMembership = UserEntraMembership.builder()
                .resourceEntraId(UUID.randomUUID())
                .userEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.ERROR)
                .build();

        service.publish(userEntraMembership, false);

        verify(entityProducer, never()).send(any(ParameterizedProducerRecord.class));
        verify(userEntraMembershipRepository, never()).save(any());
    }

    @Test
    public void shouldPublishErrorMembershipWhenForced() {
        UserEntraMembership userEntraMembership = UserEntraMembership.builder()
                .resourceEntraId(UUID.randomUUID())
                .userEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.ERROR)
                .build();

        service.publish(userEntraMembership, true);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.ADD, record.getValue().getOperation());
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @Test
    public void shouldPublishRemoveFromInactiveUserEntraMembershipAndMarkAsDeletionSent() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        UserEntraMembership userEntraMembership = UserEntraMembership.builder()
                .resourceEntraId(groupRef)
                .userEntraId(userRef)
                .membershipStatus(MembershipStatus.INACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();

        service.publish(userEntraMembership, false);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.REMOVE, record.getValue().getOperation());
        assertEquals(EntraMembershipSyncStatus.DELETION_SENT, userEntraMembership.getEntraStatus());
        assertNotNull(userEntraMembership.getDeletionSentToEntraAt());
        assertNull(userEntraMembership.getSentToEntraAt());
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ParameterizedProducerRecord<UserResourceGroupMembershipCommand> captureSentRecord() {
        ArgumentCaptor<ParameterizedProducerRecord> captor = ArgumentCaptor.forClass(ParameterizedProducerRecord.class);
        verify(entityProducer, times(1)).send(captor.capture());
        return captor.getValue();
    }
}
