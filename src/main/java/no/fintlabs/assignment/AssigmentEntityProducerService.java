package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.groupmembership.MembershipEventNames;
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

import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class AssigmentEntityProducerService {

    private final ParameterizedTemplate<ResourceGroupMembership> membershipProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final UserEntraMembershipRepository userEntraMembershipRepository;

    public AssigmentEntityProducerService(
            ParameterizedTemplateFactory producerFactory,
            EventTopicService eventTopicService,
            UserEntraMembershipRepository userEntraMembershipRepository
    ) {
        membershipProducer = producerFactory.createTemplate(ResourceGroupMembership.class);
        this.userEntraMembershipRepository = userEntraMembershipRepository;

        resourceGroupMembershipTopicNameParameters = KafkaEventTopics.topicNameParameters(MembershipEventNames.RESOURCE_GROUP_MEMBERSHIP_USER);
        eventTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaEventTopics.topicConfiguration()
        );
    }

    public void publish(FlattenedAssignment assignment) {
        publish(assignment, false);
    }

    public void publish(FlattenedAssignment assignment, boolean force) {
        if (assignment.getUserEntraMembership() != null) {
            publish(assignment.getUserEntraMembership(), force);
        } else if (isValidAssignment(assignment)) {
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
        if (flattenedAssignment.getUserEntraMembership() != null) {
            UserEntraMembership userEntraMembership = flattenedAssignment.getUserEntraMembership();
            userEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);
            publish(userEntraMembership, true);
        } else if (isValidAssignment(flattenedAssignment)) {
            logAssignment(flattenedAssignment, "Publishing deletion of flattened assignment");
            publishDeletion(flattenedAssignment.getIdentityProviderGroupObjectId(), flattenedAssignment.getIdentityProviderUserObjectId());
        }
    }

    public void publish(UserEntraMembership userEntraMembership, boolean force) {
        if (!isValidMembership(userEntraMembership)) {
            return;
        }

        if (userEntraMembership.getEntraStatus().equals(EntraStatus.ERROR) && !force) {
            log.warn("UserEntraMembership with id {} has EntraStatus ERROR. Skipping publishing to Entra.", userEntraMembership.getId());
            return;
        }

        log.info("Publishing to Entra - userEntraMembership with id: {}", userEntraMembership.getId());
        if (userEntraMembership.getMembershipStatus().equals(MembershipStatus.ACTIVE)) {
            publish(userEntraMembership.getResourceEntraId(), userEntraMembership.getUserEntraId(), OperationType.ADD);
            userEntraMembership.setEntraStatus(EntraStatus.SENT);
            userEntraMembership.setSentToEntraAt(new Date());
        } else {
            publish(userEntraMembership.getResourceEntraId(), userEntraMembership.getUserEntraId(), OperationType.REMOVE);
            userEntraMembership.setEntraStatus(EntraStatus.DELETION_SENT);
            userEntraMembership.setDeletionSentToEntraAt(new Date());
        }
        userEntraMembershipRepository.save(userEntraMembership);
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

    private boolean isValidMembership(UserEntraMembership userEntraMembership) {
        if (userEntraMembership.getResourceEntraId() == null || userEntraMembership.getUserEntraId() == null ||
            userEntraMembership.getResourceEntraId().toString().startsWith("00000000") ||
            userEntraMembership.getUserEntraId().toString().startsWith("00000000")) {

            log.warn("Publishing user Entra membership skipped for membership id {}. Missing entraId group id ({}) or user id ({})",
                    userEntraMembership.getId(),
                    userEntraMembership.getResourceEntraId(),
                    userEntraMembership.getUserEntraId()
            );

            return false;
        }

        return true;
    }

    public void publishDeletion(UUID entraIdGroupId, UUID entraIdUserId) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(OperationType.REMOVE, entraIdGroupId, entraIdUserId);

        log.info("Publishing removal to Entra - groupid: {}, userid: {}", entraIdGroupId, entraIdUserId);

        send(resourceGroupMembership);
    }

    private void publish(UUID entraIdGroupId, UUID entraIdUserId) {
        publish(entraIdGroupId, entraIdUserId, OperationType.ADD);
    }

    private void publish(UUID entraIdGroupId, UUID entraIdUserId, OperationType operationType) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(operationType, entraIdGroupId, entraIdUserId);

        log.info("Publishing {} to Entra - groupid: {}, userid: {}", operationType, entraIdGroupId, entraIdUserId);
        send(resourceGroupMembership);
    }

    private void rePublish(UUID entraIdGroupId, UUID entraIdUserId) {
        ResourceGroupMembership resourceGroupMembership = new ResourceGroupMembership(OperationType.ADD, entraIdGroupId, entraIdUserId);

        log.info("Republishing resource {} assigned to user {}", entraIdGroupId, entraIdUserId);

        send(resourceGroupMembership);
    }

    private void send(ResourceGroupMembership resourceGroupMembership) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        membershipProducer.send(
                ParameterizedProducerRecord.<ResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(resourceGroupMembership)
                        .build()
        );
    }
}
