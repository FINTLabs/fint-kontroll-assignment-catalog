package no.fintlabs.groupmembership;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AzureAdGroupMemberShipConsumerTest {

    @Mock
    private FlattenedAssignmentRepository repo;

    @Mock
    private EntityConsumerFactoryService factoryService;

    private AzureAdGroupMemberShipConsumer consumer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new AzureAdGroupMemberShipConsumer(repo);
    }

    @Test
    public void processGroupMembership_handlesDeletion() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        ConsumerRecord<String, AzureAdGroupMembership> record = new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, null);

        FlattenedAssignment flattenedAssignmentForDeletion = new FlattenedAssignment();

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(groupIdUuid, userIdUuid, false)).thenReturn(
                List.of(flattenedAssignmentForDeletion));
        when(repo.save(flattenedAssignmentForDeletion)).thenReturn(flattenedAssignmentForDeletion);

        consumer.processGroupMembership(record);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(groupIdUuid, userIdUuid, false);
        verify(repo, times(1)).save(flattenedAssignmentForDeletion);
    }

    @Test
    public void processGroupMembership_handlesUpdate() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        AzureAdGroupMembership azureAdGroupMembership = new AzureAdGroupMembership("testId", groupIdUuid, userIdUuid);

        ConsumerRecord<String, AzureAdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, azureAdGroupMembership);

        FlattenedAssignment flattenedAssignmentForUpdate = new FlattenedAssignment();

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(groupIdUuid, userIdUuid, false)).thenReturn(List.of(flattenedAssignmentForUpdate));
        when(repo.save(flattenedAssignmentForUpdate)).thenReturn(flattenedAssignmentForUpdate);

        consumer.processGroupMembership(record);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(groupIdUuid, userIdUuid, false);
        verify(repo, times(1)).save(flattenedAssignmentForUpdate);
    }


}
