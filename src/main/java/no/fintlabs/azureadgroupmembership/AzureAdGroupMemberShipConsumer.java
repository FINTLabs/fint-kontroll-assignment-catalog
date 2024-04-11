package no.fintlabs.azureadgroupmembership;

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
                        consumerRecord -> processRecord(consumerRecord, flattenedAssignmentRepository))
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("azuread-resource-group-membership")
                                         .build());
    }

    private void processRecord(ConsumerRecord<String, AzureAdGroupMembership> consumerRecord,
                               FlattenedAssignmentRepository flattenedAssignmentRepository) {
        log.info("Received azureadmembership update from topic: azuread-resource-group-membership. Groupref: {}",
                 consumerRecord.value().getAzureGroupRef());

        UUID identityProviderGroupID = UUID.fromString(consumerRecord.value().getAzureGroupRef().toString());

        flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderGroupMembershipConfirmed(
                        identityProviderGroupID, false)
                .ifPresent(flattenedAssignment -> {
                    log.info("AzureAdGroupMemberShipConsumer: Found assignment: " + flattenedAssignment.getAssignmentId());

                    flattenedAssignment.setIdentityProviderGroupMembershipConfirmed(true);
                    flattenedAssignmentRepository.save(flattenedAssignment);
                });
    }
}
