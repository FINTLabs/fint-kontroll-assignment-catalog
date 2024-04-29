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
    @Bean
    public ConcurrentMessageListenerContainer<String, AzureAdGroupMembership> azureAdMembershipConsumer(
            FlattenedAssignmentRepository flattenedAssignmentRepository,
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createFactory(
                        AzureAdGroupMembership.class,
                        consumerRecord -> processGroupMembership(consumerRecord, flattenedAssignmentRepository))
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("azuread-resource-group-membership")
                                         .build());
    }

    private void processGroupMembership(ConsumerRecord<String, AzureAdGroupMembership> azureAdGroupMembershipConsumerRecord,
                                        FlattenedAssignmentRepository flattenedAssignmentRepository) {

        AzureAdGroupMembership azureAdGroupMembership = azureAdGroupMembershipConsumerRecord.value();


        //TODO, sjekk om body er tom
        // flattenedAssignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
        if(azureAdGroupMembership == null) {
            log.info("AzureAdGroupMemberShipConsumer: Received empty body, handling as deletion. Key: {}",
                     azureAdGroupMembershipConsumerRecord.key());

            return;
        }

        log.info("Received azureadmembership update from topic: azuread-resource-group-membership. AzureAdGroupMembership groupref {} - userref {}",
                 azureAdGroupMembership.getAzureGroupRef(), azureAdGroupMembership.getAzureUserRef());

        try {
            UUID identityProviderGroupID = UUID.fromString(azureAdGroupMembership.getAzureGroupRef().toString());
            UUID identityProviderUserObjectId = UUID.fromString(azureAdGroupMembership.getAzureUserRef().toString());

            flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmed(
                            identityProviderGroupID, identityProviderUserObjectId, false)
                    .ifPresent(flattenedAssignment -> {
                        log.info("AzureAdGroupMemberShipConsumer: Found assignment mathing: " + flattenedAssignment.getAssignmentId());

                        flattenedAssignment.setIdentityProviderGroupMembershipConfirmed(true);
                        flattenedAssignmentRepository.save(flattenedAssignment);
                    });
        } catch (Exception e) {
            log.error("Failed to find matching assignment. {}", azureAdGroupMembership.getId(), e);
        }
    }
}
