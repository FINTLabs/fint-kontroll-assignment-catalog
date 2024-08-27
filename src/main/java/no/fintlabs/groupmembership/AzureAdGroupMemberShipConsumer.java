package no.fintlabs.groupmembership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.user.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
public class AzureAdGroupMemberShipConsumer {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    private final UserRepository userRepository;

    public AzureAdGroupMemberShipConsumer(FlattenedAssignmentRepository flattenedAssignmentRepository, AssigmentEntityProducerService assigmentEntityProducerService, FlattenedAssignmentService flattenedAssignmentService, UserRepository userRepository) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.userRepository = userRepository;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AzureAdGroupMembership> azureAdMembershipConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createFactory(
                        AzureAdGroupMembership.class,
                        this::processGroupMembership)
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("azuread-resource-group-membership")
                                         .build());
    }

    void processGroupMembership(ConsumerRecord<String, AzureAdGroupMembership> record) {
        AzureAdGroupMembership membership = record.value();

        if (membership == null) {
            handleDeletion(record);
        } else {
            handleUpdate(membership);
        }
    }

    private void handleDeletion(ConsumerRecord<String, AzureAdGroupMembership> record) {
        log.info("Handling deletion for empty body with key: {}", record.key());

        try {
            String[] ids = record.key().split("_");
            UUID groupId = parseUUID(ids[0]);
            UUID userId = parseUUID(ids[1]);

            List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .forEach(assignment -> {
                        log.info("Found assignment for deletion: {}", assignment.getAssignmentId());
                        assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                        flattenedAssignmentRepository.saveAndFlush(assignment);
                    });

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .forEach(assignment -> {
                        log.info("Found inconsistent assignment on deletion, updating and publishing. {}", assignment.getAssignmentId());
                        assigmentEntityProducerService.publish(assignment);
                    });

            log.info("Finished handling deletion for azureref {}", record.key());
        } catch (Exception e) {
            log.error("Failed to handle deletion for azureref {}. Error: {}", record.key(), e.getMessage());
        }
    }

    private void handleUpdate(AzureAdGroupMembership membership) {
        log.debug("Received update with groupref {} - userref {}", membership.getAzureGroupRef(), membership.getAzureUserRef());

        try {
            UUID groupId = membership.getAzureGroupRef();
            UUID userId = membership.getAzureUserRef();

            List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);

            if(flattenedAssignments.isEmpty()) {
                assigmentEntityProducerService.publishDeletion(groupId, userId);
                log.info("User assignment not found with id: {}, removing user from group: {} in azure", userId, groupId);
            } else {
                handleValidMembershipUpdate(membership, groupId, userId);
            }

        } catch (Exception e) {
            log.error("Failed to handle update for groupref {} - userref {}. Error: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(), e.getMessage());
        }
    }

    private void handleValidMembershipUpdate(AzureAdGroupMembership membership, UUID groupId, UUID userId) {
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);

        List<FlattenedAssignment> assignmentsToUpdate = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipConfirmed())
                .peek(assignment -> {
                    log.info("Received update with groupref {} - userref {}, saving as confirmed on assignmentId: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(),
                             assignment.getAssignmentId());
                    assignment.setIdentityProviderGroupMembershipConfirmed(true);
                })
                .toList();

        List<FlattenedAssignment> assignmentsToDelete = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                .peek(assignment -> {
                    log.info("Found inconsistent assignment on update, updating and publishing. {}", assignment.getAssignmentId());
                })
                .toList();

        if (!assignmentsToUpdate.isEmpty()) {
            flattenedAssignmentService.saveFlattenedAssignmentsBatch(assignmentsToUpdate, false);
        }

        if (!assignmentsToDelete.isEmpty()) {
            assignmentsToDelete.forEach(assigmentEntityProducerService::publishDeletion);
        }
    }

    private UUID parseUUID(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", uuidString, e);
            return null;
        }
    }
}
