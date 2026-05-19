package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.groupmembership.ResourceGroupMembership;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EntityTopicService;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class AssigmentEntityProducerService {

    private final ParameterizedTemplate<ResourceGroupMembership> entityProducer;
    private final EntityTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final EntityTopicNameParameters fullResourceGroupMembershipTopicNameParameters;

    public AssigmentEntityProducerService(
            ParameterizedTemplateFactory entityProducerFactory,
            EntityTopicService entityTopicService
    ) {
        entityProducer = entityProducerFactory.createTemplate(ResourceGroupMembership.class);

        resourceGroupMembershipTopicNameParameters = KafkaEntityTopics.topicNameParameters("resource-group-membership");
        entityTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaEntityTopics.compactedTopicConfiguration()
        );

        fullResourceGroupMembershipTopicNameParameters = KafkaEntityTopics.topicNameParameters("full-resource-group-membership");
        entityTopicService.createOrModifyTopic(
                fullResourceGroupMembershipTopicNameParameters,
                KafkaEntityTopics.compactedTopicConfiguration(Duration.ofMinutes(5))
        );
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
                ParameterizedProducerRecord.<ResourceGroupMembership>builder()
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
                ParameterizedProducerRecord.<ResourceGroupMembership>builder()
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
                ParameterizedProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(fullResourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }
}
