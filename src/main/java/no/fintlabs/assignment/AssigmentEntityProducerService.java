package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.groupmembership.ResourceGroupMembership;
import no.fintlabs.kafka.model.ParameterizedProducerRecord;
import no.fintlabs.kafka.producing.ParameterizedTemplate;
import no.fintlabs.kafka.producing.ParameterizedTemplateFactory;
import no.fintlabs.kafka.topic.EntityTopicService;
import no.fintlabs.kafka.topic.configuration.CleanupFrequency;
import no.fintlabs.kafka.topic.configuration.EntityTopicConfiguration;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class AssigmentEntityProducerService {

    private final ParameterizedTemplate<ResourceGroupMembership> resourceGroupMembershipTemplate;
    private final EntityTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final EntityTopicNameParameters fullResourceGroupMembershipTopicNameParameters;

    public AssigmentEntityProducerService(
            ParameterizedTemplateFactory parameterizedTemplateFactory,
            EntityTopicService entityTopicService
    ) {
        resourceGroupMembershipTemplate = parameterizedTemplateFactory.createTemplate(ResourceGroupMembership.class);

        TopicNamePrefixParameters topicNamePrefixParameters = TopicNamePrefixParameters.builder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();

        resourceGroupMembershipTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("resource-group-membership")
                .build();
        entityTopicService.createOrModifyTopic(resourceGroupMembershipTopicNameParameters,
                                               EntityTopicConfiguration.builder()
                                                       .lastValueRetainedForever()
                                                       .nullValueRetentionTime(Duration.ofDays(7))
                                                       .cleanupFrequency(CleanupFrequency.NORMAL)
                                                       .build());

        fullResourceGroupMembershipTopicNameParameters = EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(topicNamePrefixParameters)
                .resourceName("full-resource-group-membership")
                .build();

        entityTopicService.createOrModifyTopic(fullResourceGroupMembershipTopicNameParameters, EntityTopicConfiguration.builder()
                .lastValueRetentionTime(Duration.ofMinutes(5))
                .nullValueRetentionTime(Duration.ofDays(7))
                .cleanupFrequency(CleanupFrequency.NORMAL)
                .build());
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

    private void publishDeletion(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();

        resourceGroupMembershipTemplate.send(
                new ParameterizedProducerRecord<>(
                        resourceGroupMembershipTopicNameParameters,
                        null, key, null)
        );
    }

    private void publish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        ResourceGroupMembership azureAdGroupMembership = new ResourceGroupMembership(key, azureAdGroupId, azureUserId);

        log.info("Publiserer ressurs " + azureAdGroupId + " tildelt bruker " + azureUserId);

        resourceGroupMembershipTemplate.send(
                new ParameterizedProducerRecord<>(
                        resourceGroupMembershipTopicNameParameters, null, key, azureAdGroupMembership)
        );
    }

    private void rePublish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        ResourceGroupMembership azureAdGroupMembership = new ResourceGroupMembership(key, azureAdGroupId, azureUserId);

        log.info("Republishing resource {} assigned to user {}", azureAdGroupId, azureUserId);

        resourceGroupMembershipTemplate.send(
                new ParameterizedProducerRecord(fullResourceGroupMembershipTopicNameParameters, null, key, azureAdGroupMembership)
        );
    }
}
