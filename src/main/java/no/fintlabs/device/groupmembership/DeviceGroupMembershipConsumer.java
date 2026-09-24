package no.fintlabs.device.groupmembership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceGroupMembershipConsumer {

    private final DeviceGroupMembershipService deviceGroupMembershipService;
    private final RetryTemplate kafkaRetryTemplate;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, DeviceGroupMembership> deviceGroupMembershipConsumerConfiguration(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {
        return entityConsumerFactoryService
                .createRecordListenerContainerFactory(
                        DeviceGroupMembership.class,
                        this::processWithRetry,
                        KafkaEntityTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEntityTopics.topicNameParameters("kontroll-device-group-membership"));
    }
    private void processWithRetry(ConsumerRecord<String, DeviceGroupMembership> record) {
        kafkaRetryTemplate.execute(context -> {
            processDeviceGroupMembership(record);
            return null;
        });
    }
    private void processDeviceGroupMembership(ConsumerRecord<String, DeviceGroupMembership> stringDeviceGroupMembershipConsumerRecord) {
        DeviceGroupMembership incomingMembership = stringDeviceGroupMembershipConsumerRecord.value();
        if (incomingMembership != null) {
            deviceGroupMembershipService.saveOrUpdate(incomingMembership);
        }
    }


}
