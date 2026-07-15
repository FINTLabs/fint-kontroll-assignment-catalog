package no.fintlabs.device;

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
public class DeviceConsumer {

    private final DeviceService deviceService;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, Device> deviceConsumerConfiguration(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {
        return entityConsumerFactoryService
                .createRecordListenerContainerFactory(
                        Device.class,
                        this::processDevice,
                        KafkaEntityTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEntityTopics.topicNameParameters("kontroll-device"));
    }

    private void processDevice(ConsumerRecord<String, Device> stringDeviceConsumerRecord) {
        Device incomingDevice = stringDeviceConsumerRecord.value();
        if (incomingDevice == null) {
            deviceService.deleteDevice(stringDeviceConsumerRecord.key());
        } else {
            deviceService.saveOrUpdate(incomingDevice);
        }
    }


}
