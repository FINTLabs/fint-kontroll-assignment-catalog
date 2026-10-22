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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assignment.setIdentityProviderGroupObjectId(groupRef);
        assignment.setIdentityProviderUserObjectId(userRef);

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
        assignment.setIdentityProviderGroupObjectId(groupRef);
        assignment.setIdentityProviderUserObjectId(userRef);

        service.publishDeletion(assignment);

        ParameterizedProducerRecord<UserResourceGroupMembershipCommand> record = captureSentRecord();
        assertEquals(OperationType.REMOVE, record.getValue().getOperation());
        assertEquals(groupRef, record.getValue().getEntraGroupRef());
        assertEquals(userRef, record.getValue().getEntraUserRef());
    }

    @Test
    public void shouldNotPublishDeletion_invalidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publishDeletion(assignment);

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
        verify(userEntraMembershipRepository).save(userEntraMembership);
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
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ParameterizedProducerRecord<UserResourceGroupMembershipCommand> captureSentRecord() {
        ArgumentCaptor<ParameterizedProducerRecord> captor = ArgumentCaptor.forClass(ParameterizedProducerRecord.class);
        verify(entityProducer, times(1)).send(captor.capture());
        return captor.getValue();
    }
}
