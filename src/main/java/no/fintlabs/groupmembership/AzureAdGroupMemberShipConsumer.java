package no.fintlabs.groupmembership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.UUID;

@Slf4j
@Configuration
public class AzureAdGroupMemberShipConsumer {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    public AzureAdGroupMemberShipConsumer(FlattenedAssignmentRepository flattenedAssignmentRepository) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
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

        String[] ids = record.key().split("_");
        UUID groupId = parseUUID(ids[0]);
        UUID userId = parseUUID(ids[1]);

        flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNull(groupId, userId)
                .ifPresent(assignment -> {
                    log.info("Found assignment for deletion: {}", assignment.getAssignmentId());
                    assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                    flattenedAssignmentRepository.save(assignment);
                });
    }

    private void handleUpdate(AzureAdGroupMembership membership) {
        log.info("Received update with groupref {} - userref {}", membership.getAzureGroupRef(), membership.getAzureUserRef());

        UUID groupId = parseUUID(membership.getAzureGroupRef().toString());
        UUID userId = parseUUID(membership.getAzureUserRef().toString());

        flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmed(
                        groupId, userId, false)
                .ifPresent(assignment -> {
                    log.info("Found matching assignment: {}", assignment.getAssignmentId());
                    assignment.setIdentityProviderGroupMembershipConfirmed(true);
                    flattenedAssignmentRepository.save(assignment);
                });
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
