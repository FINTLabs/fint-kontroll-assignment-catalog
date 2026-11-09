package no.fintlabs.assignment.user.entra;

import lombok.RequiredArgsConstructor;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.entra.membership.event.MembershipEventNames;
import no.fintlabs.kafka.KafkaEventTopics;
import no.fintlabs.kafka.KafkaTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;


@Configuration
@RequiredArgsConstructor
public class EntraUserGroupMembershipConsumer {

    private static final int MAX_POLL_RECORDS = 100;

    private final EntraUserGroupMembershipProcessor processor;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraUserGroupMembershipResult> entraIdMembershipConsumer(
            ParameterizedListenerContainerFactoryService eventConsumerFactoryService
    ) {
        return eventConsumerFactoryService.createBatchListenerContainerFactory(
                        EntraUserGroupMembershipResult.class,
                        processor::processBatch,
                        KafkaTopics.listenerConfigurationWithMaxPollRecords(MAX_POLL_RECORDS),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEventTopics.topicNameParameters(MembershipEventNames.ENTRA_USER_GROUP_MEMBERSHIP));
    }
}
