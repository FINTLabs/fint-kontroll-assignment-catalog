package no.fintlabs.groupmembership;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

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
            EntityConsumerFactoryService entityConsumerFactoryService,
            ConcurrentKafkaListenerContainerFactory<String, AzureAdGroupMembership> factory
    ) {
        factory.setConcurrency(4);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        return entityConsumerFactoryService.createFactory(
                        AzureAdGroupMembership.class,
                        this::processGroupMembership)
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("azuread-resource-group-membership")
                                         .build());
    }

    @Transactional
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

            flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(groupId, userId, false)
                    .forEach(assignment -> {
                        log.info("Found assignment for deletion: {}", assignment.getAssignmentId());
                        assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                        flattenedAssignmentRepository.save(assignment);
                    });
        } catch (Exception e) {
            log.error("Failed to handle deletion for azureref {}. Error: {}", record.key(), e.getMessage());
        }
    }

    private void handleUpdate(AzureAdGroupMembership membership) {
        log.debug("Received update with groupref {} - userref {}", membership.getAzureGroupRef(), membership.getAzureUserRef());

        try {
            UUID groupId = parseUUID(membership.getAzureGroupRef().toString());
            UUID userId = parseUUID(membership.getAzureUserRef().toString());

            flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(
                            groupId, userId, false)
                    .forEach(assignment -> {
                        log.debug("Received update with groupref {} - userref {}, saving as confirmed on assignmentId: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(), assignment.getAssignmentId());
                        assignment.setIdentityProviderGroupMembershipConfirmed(true);
                        flattenedAssignmentRepository.save(assignment);
                    });
        } catch (Exception e) {
            log.error("Failed to handle update for groupref {} - userref {}. Error: {}", membership.getAzureGroupRef(), membership.getAzureUserRef(), e.getMessage());
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
