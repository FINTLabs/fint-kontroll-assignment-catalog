package no.fintlabs.assignment;

import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.groupmembership.OperationType;
import no.fintlabs.groupmembership.ResourceGroupMembership;
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

    private AssigmentEntityProducerService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(entityProducerFactory.createTemplate(any())).thenReturn(entityProducer);
        service = new AssigmentEntityProducerService(entityProducerFactory, eventTopicService, userEntraMembershipRepository);
    }

    @Test
    public void shouldPublish_validAssignment() {
        UUID groupRef = UUID.randomUUID();
        UUID userRef = UUID.randomUUID();
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setIdentityProviderGroupObjectId(groupRef);
        assignment.setIdentityProviderUserObjectId(userRef);

        service.publish(assignment);

        ParameterizedProducerRecord<ResourceGroupMembership> record = captureSentRecord();
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

        ParameterizedProducerRecord<ResourceGroupMembership> record = captureSentRecord();
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
                .entraStatus(EntraStatus.NOT_SENT)
                .build();

        service.publish(userEntraMembership, false);

        ParameterizedProducerRecord<ResourceGroupMembership> record = captureSentRecord();
        assertEquals(OperationType.ADD, record.getValue().getOperation());
        assertEquals(EntraStatus.SENT, userEntraMembership.getEntraStatus());
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
                .entraStatus(EntraStatus.NOT_SENT)
                .build();

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.publish(userEntraMembership, false);

            assertEquals(EntraStatus.SENT, userEntraMembership.getEntraStatus());
            verify(userEntraMembershipRepository).save(userEntraMembership);
            verify(entityProducer, never()).send(any(ParameterizedProducerRecord.class));

            List<TransactionSynchronization> synchronizations =
                    new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
            assertEquals(1, synchronizations.size());

            synchronizations.forEach(TransactionSynchronization::afterCommit);

            ParameterizedProducerRecord<ResourceGroupMembership> record = captureSentRecord();
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
                .entraStatus(EntraStatus.MEMBERSHIP_CONFIRMED)
                .build();

        service.publish(userEntraMembership, false);

        ParameterizedProducerRecord<ResourceGroupMembership> record = captureSentRecord();
        assertEquals(OperationType.REMOVE, record.getValue().getOperation());
        assertEquals(EntraStatus.DELETION_SENT, userEntraMembership.getEntraStatus());
        verify(userEntraMembershipRepository).save(userEntraMembership);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ParameterizedProducerRecord<ResourceGroupMembership> captureSentRecord() {
        ArgumentCaptor<ParameterizedProducerRecord> captor = ArgumentCaptor.forClass(ParameterizedProducerRecord.class);
        verify(entityProducer, times(1)).send(captor.capture());
        return captor.getValue();
    }
}
