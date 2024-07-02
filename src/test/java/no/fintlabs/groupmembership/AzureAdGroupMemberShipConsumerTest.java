package no.fintlabs.groupmembership;

import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AzureAdGroupMemberShipConsumerTest {

    @Mock
    private FlattenedAssignmentRepository repo;

    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;

    @Mock
    private EntityConsumerFactoryService factoryService;

    private AzureAdGroupMemberShipConsumer consumer;

    @Mock
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        consumer = new AzureAdGroupMemberShipConsumer(repo, assigmentEntityProducerService, flattenedAssignmentService);
    }

    @Test
    public void processGroupMembership_handlesDeletion() {
        UUID groupIdUuid = UUID.randomUUID();
        UUID userIdUuid = UUID.randomUUID();

        String groupId = groupIdUuid.toString();
        String userId = userIdUuid.toString();

        ConsumerRecord<String, AzureAdGroupMembership> record = new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, null);

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

        AzureAdGroupMembership azureAdGroupMembership = new AzureAdGroupMembership("testId", groupIdUuid, userIdUuid);

        ConsumerRecord<String, AzureAdGroupMembership> record =
                new ConsumerRecord<>("topic", 1, 1, groupId + "_" + userId, azureAdGroupMembership);

        FlattenedAssignment flattenedAssignmentForUpdate = new FlattenedAssignment();

        when(repo.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid)).thenReturn(List.of(flattenedAssignmentForUpdate));

        consumer.processGroupMembership(record);

        verify(repo, times(1)).findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupIdUuid, userIdUuid);
        verify(flattenedAssignmentService, times(1)).saveFlattenedAssignmentsBatch(List.of(flattenedAssignmentForUpdate), false);
    }


}
