package no.fintlabs.groupmembership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
public class AzureAdGroupMemberShipConsumer {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;


    public AzureAdGroupMemberShipConsumer(FlattenedAssignmentRepository flattenedAssignmentRepository, AssigmentEntityProducerService assigmentEntityProducerService, FlattenedAssignmentService flattenedAssignmentService) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.flattenedAssignmentService = flattenedAssignmentService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AzureAdGroupMembership> azureAdMembershipConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {

        ConcurrentMessageListenerContainer<String, AzureAdGroupMembership> container = entityConsumerFactoryService.createFactory(
                        AzureAdGroupMembership.class,
                        this::processGroupMembership)
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("azuread-resource-group-membership")
                                         .build());
        container.setConcurrency(5);
        return container;
    }

    @Async
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
                        log.info("Found assignment for deletion. flattenedassignmentId: {}", assignment.getId());
                        assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                        flattenedAssignmentRepository.saveAndFlush(assignment);
                    });

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .forEach(assignment -> {
                        log.info("Found inconsistent assignment on deletion, updating and publishing. flattenedassignmentId: {}", assignment.getId());
                        assigmentEntityProducerService.publish(assignment);
                    });

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && assignment.isIdentityProviderGroupMembershipConfirmed())
                    .forEach(assignment -> {
                        log.info("Found inconsistent delete, recreating assignment in Azure. flattenedassignmentId: {}", assignment.getId());
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
                handleValidMembershipUpdate(membership, flattenedAssignments);
            }

        } catch (Exception e) {
            log.error("Failed to handle update for groupref {} - userref {}. Error: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(), e.getMessage());
        }
    }

    private void handleValidMembershipUpdate(AzureAdGroupMembership membership, List<FlattenedAssignment> flattenedAssignments) {
        List<FlattenedAssignment> assignmentsToUpdate = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipConfirmed())
                .peek(assignment -> {
                    log.info("Received update with groupref {} - userref {}, saving as confirmed on flattenedassignmentId: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(),
                             assignment.getId());
                    assignment.setIdentityProviderGroupMembershipConfirmed(true);
                })
                .toList();

        List<FlattenedAssignment> assignmentsToDelete = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                .peek(assignment -> {
                    log.info("Found inconsistent assignment on update, updating and publishing. flattenedassignmentId: {}", assignment.getId());
                })
                .toList();

        if (!assignmentsToUpdate.isEmpty()) {
            flattenedAssignmentService.saveFlattenedAssignmentsBatch(assignmentsToUpdate);
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
