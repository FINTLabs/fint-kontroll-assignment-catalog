package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.groupmembership.ResourceGroupMembership;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AssigmentEntityProducerService {

    private final EntityProducer<ResourceGroupMembership> entityProducer;
    private final EntityTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final EntityTopicNameParameters fullResourceGroupMembershipTopicNameParameters;

    public AssigmentEntityProducerService(
            EntityProducerFactory entityProducerFactory,
            EntityTopicService entityTopicService
    ) {
        entityProducer = entityProducerFactory.createProducer(ResourceGroupMembership.class);

        resourceGroupMembershipTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resource-group-membership")
                .build();
        entityTopicService.ensureTopic(resourceGroupMembershipTopicNameParameters, 0);

        fullResourceGroupMembershipTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("full-resource-group-membership")
                .build();
        entityTopicService.ensureTopic(fullResourceGroupMembershipTopicNameParameters, 300000); // 5 minutes retention
    }

    public void publish(FlattenedAssignment assignment) {
        if (isValidAssignment(assignment)) {
            logAssignment(assignment, "Publishing flattened assignment");
            publish(assignment.getIdentityProviderGroupObjectId(), assignment.getIdentityProviderUserObjectId());
        }
    }

    public void rePublish(FlattenedAssignment assignment) {
        if (isValidAssignment(assignment)) {
            logAssignment(assignment, "Republishing flattened assignment");
            rePublish(assignment.getIdentityProviderGroupObjectId(), assignment.getIdentityProviderUserObjectId());
        }
    }

    public void publishDeletion(FlattenedAssignment flattenedAssignment) {
        if (isValidAssignment(flattenedAssignment)) {
            logAssignment(flattenedAssignment, "Publishing deletion of flattened assignment");
            publishDeletion(flattenedAssignment.getIdentityProviderGroupObjectId(), flattenedAssignment.getIdentityProviderUserObjectId());
        }
    }

    private void logAssignment(FlattenedAssignment assignment, String message) {
        if (assignment.getResourceRef() == null) {
            log.warn("Publishing flattened assignment {}. ResourceRef is null", assignment.getId());
        }

        log.info("{} with id {}, assignmentid {}, azuread-groupId {}, azuread-userId {}, resourceRef {}",
                 message,
                 assignment.getId(),
                 assignment.getAssignmentId(),
                 assignment.getIdentityProviderGroupObjectId(),
                 assignment.getIdentityProviderUserObjectId(),
                 assignment.getResourceRef());
    }

    private boolean isValidAssignment(FlattenedAssignment assignment) {
        if (assignment.getIdentityProviderGroupObjectId() == null || assignment.getIdentityProviderUserObjectId() == null ||
            assignment.getIdentityProviderGroupObjectId().toString().startsWith("00000000") ||
            assignment.getIdentityProviderUserObjectId().toString().startsWith("00000000")) {

            log.warn("Publishing flattened assignment skipped for assignmentid {}. ResourceRef {}. Missing azuread group id ({}) or user " +
                     "id ({})",
                     assignment.getId(),
                     assignment.getResourceRef(),
                     assignment.getIdentityProviderGroupObjectId(),
                     assignment.getIdentityProviderUserObjectId()
            );

            return false;
        }

        return true;
    }

    public void publishDeletion(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();

        entityProducer.send(
                EntityProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(null)
                        .build()
        );
    }

    private void publish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        ResourceGroupMembership azureAdGroupMembership = new ResourceGroupMembership(key, azureAdGroupId, azureUserId);

        log.info("Publishing to Azure - groupid: {}, userid: {}", azureAdGroupId, azureUserId);

        entityProducer.send(
                EntityProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }

    private void rePublish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        ResourceGroupMembership azureAdGroupMembership = new ResourceGroupMembership(key, azureAdGroupId, azureUserId);

        log.info("Republishing resource {} assigned to user {}", azureAdGroupId, azureUserId);

        entityProducer.send(
                EntityProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(fullResourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }
}
