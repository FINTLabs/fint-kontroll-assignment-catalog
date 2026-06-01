package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.groupmembership.OperationType;
import no.fintlabs.groupmembership.ResourceGroupMembership;
import no.fintlabs.kafka.KafkaEventTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class AssigmentEntityProducerService {

    private final ParameterizedTemplate<ResourceGroupMembership> membershipProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;

    public AssigmentEntityProducerService(
            ParameterizedTemplateFactory producerFactory,
            EventTopicService eventTopicService
    ) {
        membershipProducer = producerFactory.createTemplate(ResourceGroupMembership.class);

        resourceGroupMembershipTopicNameParameters = KafkaEventTopics.topicNameParameters("resource-group-membership-user");
        eventTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaEventTopics.topicConfiguration()
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

        log.info("{} with id {}, assignmentid {}, entraId-groupId {}, entraId-userId {}, resourceRef {}",
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

            log.warn("Publishing flattened assignment skipped for assignmentid {}. ResourceRef {}. Missing entraId group id ({}) or user " +
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

    public void publishDeletion(UUID entraGroupId, UUID entraUserId) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(OperationType.REMOVE, entraGroupId, entraUserId);

        log.info("Publishing removal to Entra - groupid: {}, userid: {}", entraGroupId, entraUserId);

        send(resourceGroupMembership);
    }

    private void publish(UUID entraGroupId, UUID entraUserId) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(OperationType.ADD, entraGroupId, entraUserId);

        log.info("Publishing addition to Entra - groupid: {}, userid: {}", entraGroupId, entraUserId);

        send(resourceGroupMembership);
    }

    private void rePublish(UUID entraGroupId, UUID entraUserId) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(OperationType.ADD, entraGroupId, entraUserId);

        log.info("Republishing resource {} assigned to user {}", entraGroupId, entraUserId);

        send(resourceGroupMembership);
    }

    private void send(ResourceGroupMembership resourceGroupMembership) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);        membershipProducer.send(
                ParameterizedProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(resourceGroupMembership)
                        .build()
        );
    }
}
