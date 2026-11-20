package no.fintlabs.assignment.device.entra;

import lombok.RequiredArgsConstructor;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.entra.membership.event.MembershipEventNames;
import no.fintlabs.kafka.KafkaTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;


@Configuration
@RequiredArgsConstructor
public class EntraGroupDeviceMembershipConsumer {

    private static final int MAX_POLL_RECORDS = 100;

    private final EntraGroupDeviceMembershipProcessor processor;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraDeviceGroupMembership> entraDeviceMembershipConsumer(
            ParameterizedListenerContainerFactoryService eventConsumerFactoryService
    ) {
        return eventConsumerFactoryService.createBatchListenerContainerFactory(
                        EntraDeviceGroupMembership.class,
                        processor::processBatch,
                        KafkaTopics.listenerConfigurationWithMaxPollRecords(MAX_POLL_RECORDS),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaTopics.eventTopicNameParameters(MembershipEventNames.ENTRA_DEVICE_GROUP_MEMBERSHIP));
    }
}
