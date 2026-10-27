package no.fintlabs.assignment.user.entra;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.entra.membership.event.MembershipEventNames;
import no.fintlabs.entra.membership.event.OperationType;
import no.fintlabs.entra.membership.event.UserResourceGroupMembershipCommand;
import no.fintlabs.kafka.KafkaTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class UserEntraMembershipPublisher {

    private final ParameterizedTemplate<UserResourceGroupMembershipCommand> membershipProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final UserEntraMembershipRepository userEntraMembershipRepository;

    public UserEntraMembershipPublisher(
            ParameterizedTemplateFactory producerFactory,
            EventTopicService eventTopicService,
            UserEntraMembershipRepository userEntraMembershipRepository
    ) {
        membershipProducer = producerFactory.createTemplate(UserResourceGroupMembershipCommand.class);
        this.userEntraMembershipRepository = userEntraMembershipRepository;

        resourceGroupMembershipTopicNameParameters = KafkaTopics.eventTopicNameParameters(MembershipEventNames.RESOURCE_GROUP_MEMBERSHIP_USER);
        eventTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaTopics.eventTopicConfiguration()
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

    public void rePublish(UserEntraMembership userEntraMembership) {
        if (isValidMembership(userEntraMembership)) {
            log.info("Republishing user Entra membership with id: {}", userEntraMembership.getId());
            rePublish(userEntraMembership.getResourceEntraId(), userEntraMembership.getUserEntraId());
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

        if (userEntraMembership.getEntraStatus().equals(EntraMembershipSyncStatus.ERROR) && !force) {
            log.warn("UserEntraMembership with id {} has EntraMembershipSyncStatus ERROR. Skipping publishing to Entra.", userEntraMembership.getId());
            return;
        }

        log.info("Publishing to Entra - userEntraMembership with id: {}", userEntraMembership.getId());
        if (userEntraMembership.getMembershipStatus().equals(MembershipStatus.ACTIVE)) {
            userEntraMembership.setEntraStatus(EntraMembershipSyncStatus.SENT);
            userEntraMembership.setSentToEntraAt(new Date());
            userEntraMembershipRepository.save(userEntraMembership);
            publish(userEntraMembership.getResourceEntraId(), userEntraMembership.getUserEntraId(), OperationType.ADD);
        } else {
            userEntraMembership.setEntraStatus(EntraMembershipSyncStatus.DELETION_SENT);
            userEntraMembership.setDeletionSentToEntraAt(new Date());
            userEntraMembershipRepository.save(userEntraMembership);
            publish(userEntraMembership.getResourceEntraId(), userEntraMembership.getUserEntraId(), OperationType.REMOVE);
        }
    }

    private void logAssignment(FlattenedAssignment assignment, String message) {
        log.info("{} with id {}, assignmentid {}, entraId-groupId {}, entraId-userId {}, resourceRef {}",
                 message,
                 assignment.getId(),
                 assignment.getAssignmentId(),
                 assignment.getIdentityProviderGroupObjectId(),
                 assignment.getIdentityProviderUserObjectId(),
                 assignment.getResourceRef());
    }

    private boolean isValidAssignment(FlattenedAssignment assignment) {
        if (assignment.getIdentityProviderGroupObjectId().toString().startsWith("00000000") ||
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
        UserResourceGroupMembershipCommand resourceGroupMembership = new UserResourceGroupMembershipCommand(OperationType.REMOVE, entraIdGroupId, entraIdUserId);

        log.info("Publishing removal to Entra - groupid: {}, userid: {}", entraIdGroupId, entraIdUserId);

        send(resourceGroupMembership);
    }

    private void publish(UUID entraIdGroupId, UUID entraIdUserId) {
        publish(entraIdGroupId, entraIdUserId, OperationType.ADD);
    }

    private void publish(UUID entraIdGroupId, UUID entraIdUserId, OperationType operationType) {
        UserResourceGroupMembershipCommand resourceGroupMembership = new UserResourceGroupMembershipCommand(operationType, entraIdGroupId, entraIdUserId);

        log.info("Publishing {} to Entra - groupid: {}, userid: {}", operationType, entraIdGroupId, entraIdUserId);
        send(resourceGroupMembership);
    }

    private void rePublish(UUID entraIdGroupId, UUID entraIdUserId) {
        UserResourceGroupMembershipCommand resourceGroupMembership = new UserResourceGroupMembershipCommand(OperationType.ADD, entraIdGroupId, entraIdUserId);

        log.info("Republishing resource {} assigned to user {}", entraIdGroupId, entraIdUserId);

        send(resourceGroupMembership);
    }

    private void send(UserResourceGroupMembershipCommand resourceGroupMembership) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(resourceGroupMembership);
                }
            });
            return;
        }

        sendNow(resourceGroupMembership);
    }

    private void sendNow(UserResourceGroupMembershipCommand resourceGroupMembership) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        membershipProducer.send(
                ParameterizedProducerRecord.<UserResourceGroupMembershipCommand>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(resourceGroupMembership)
                        .build()
        );
    }
}
