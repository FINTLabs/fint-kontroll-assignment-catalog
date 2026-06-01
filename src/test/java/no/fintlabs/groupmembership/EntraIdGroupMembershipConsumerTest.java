package no.fintlabs.groupmembership;

import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EntraIdGroupMembershipConsumerTest {

    @Mock
    private FlattenedAssignmentRepository repo;

    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;

    private EntraIdGroupMembershipConsumer consumer;

    @Mock
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @Mock
    private KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new EntraIdGroupMembershipConsumer(repo, assigmentEntityProducerService, flattenedAssignmentService, kafkaConsumerConfigurationDefaults);
    }

    @Test
    public void processGroupMembership_handlesDeletion() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.REMOVED, groupIdUuid, userIdUuid);

        ConsumerRecord<String, EntraIdGroupMembership> record = new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, entraIdGroupMembership);

        FlattenedAssignment flattenedAssignmentForDeletion = new FlattenedAssignment();
        flattenedAssignmentForDeletion.setAssignmentTerminationDate(new Date());

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid)).thenReturn(
                List.of(flattenedAssignmentForDeletion));
        when(repo.saveAndFlush(flattenedAssignmentForDeletion)).thenReturn(flattenedAssignmentForDeletion);

        consumer.processGroupMembership(record);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid);
        verify(repo, times(1)).saveAndFlush(flattenedAssignmentForDeletion);
    }

    @Test
    public void processGroupMembership_handlesUpdate() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.ADDED, groupIdUuid, userIdUuid);

        ConsumerRecord<String, EntraIdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, entraIdGroupMembership);

        FlattenedAssignment flattenedAssignmentForUpdate = new FlattenedAssignment();

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid)).thenReturn(List.of(flattenedAssignmentForUpdate));

        consumer.processGroupMembership(record);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid);
        verify(flattenedAssignmentService, times(1)).saveFlattenedAssignmentsBatch(List.of(flattenedAssignmentForUpdate));
        assertTrue(flattenedAssignmentForUpdate.isIdentityProviderGroupMembershipConfirmed());
    }

    @Test
    public void processGroupMembership_handlesUpdate_toDelete() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.ADDED, groupIdUuid, userIdUuid);

        ConsumerRecord<String, EntraIdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, entraIdGroupMembership);

        FlattenedAssignment flattenedAssignmentForUpdate = new FlattenedAssignment();

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid)).thenReturn(List.of());

        consumer.processGroupMembership(record);

        verify(assigmentEntityProducerService, times(1)).publishDeletion(groupIdUuid, userIdUuid);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid);
        verify(flattenedAssignmentService, times(0)).saveAndPublishFlattenedAssignmentsBatch(List.of(flattenedAssignmentForUpdate), false);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForActiveAssignmentAsConfirmed() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();
        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.NO_CHANGES, groupIdUuid, userIdUuid);
        ConsumerRecord<String, EntraIdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupIdUuid + "_" + userIdUuid, entraIdGroupMembership);

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setIdentityProviderGroupMembershipConfirmed(false);

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid))
                .thenReturn(List.of(flattenedAssignment));

        consumer.processGroupMembership(record);

        assertTrue(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
        assertFalse(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
        verify(flattenedAssignmentService, times(1)).saveFlattenedAssignmentsBatch(List.of(flattenedAssignment));
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_handlesNoChangesForTerminatedAssignmentAsDeletionConfirmed() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();
        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.NO_CHANGES, groupIdUuid, userIdUuid);
        ConsumerRecord<String, EntraIdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupIdUuid + "_" + userIdUuid, entraIdGroupMembership);

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setAssignmentTerminationDate(new Date());
        flattenedAssignment.setIdentityProviderGroupMembershipDeletionConfirmed(false);

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid))
                .thenReturn(List.of(flattenedAssignment));

        consumer.processGroupMembership(record);

        assertFalse(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
        assertTrue(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
        verify(flattenedAssignmentService, times(1)).saveFlattenedAssignmentsBatch(List.of(flattenedAssignment));
        verifyNoInteractions(assigmentEntityProducerService);
    }

    @Test
    public void processGroupMembership_doesNotMutateAssignmentsForFailedResult() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();
        EntraIdGroupMembership entraIdGroupMembership = new EntraIdGroupMembership(EntraStatus.FAILED, groupIdUuid, userIdUuid);
        ConsumerRecord<String, EntraIdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupIdUuid + "_" + userIdUuid, entraIdGroupMembership);

        consumer.processGroupMembership(record);

        verify(repo, never()).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(any(), any());
        verify(flattenedAssignmentService, never()).saveFlattenedAssignmentsBatch(any());
        verifyNoInteractions(assigmentEntityProducerService);
    }
}
