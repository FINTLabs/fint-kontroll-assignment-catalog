package no.fintlabs.device.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceGroupConsumer {

    private final DeviceGroupService deviceGroupService;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, DeviceGroup> deviceGroupConsumerConfiguration(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {
        return entityConsumerFactoryService
                .createRecordListenerContainerFactory(
                        DeviceGroup.class,
                        this::processDeviceGroup,
                        KafkaEntityTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEntityTopics.topicNameParameters("kontroll-device-group"));
    }

    private void processDeviceGroup(ConsumerRecord<String, DeviceGroup> stringDeviceGroupConsumerRecord) {
        DeviceGroup incomingDevice = stringDeviceGroupConsumerRecord.value();
        if (incomingDevice != null) {
            deviceGroupService.saveOrUpdate(incomingDevice);
        }
    }


}
