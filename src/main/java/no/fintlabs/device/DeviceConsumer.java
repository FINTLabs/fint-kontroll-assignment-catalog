package no.fintlabs.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceConsumer {

    private final DeviceService deviceService;

    @Bean
    public ConcurrentMessageListenerContainer<String, Device> deviceConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters kontrolldevice = EntityTopicNameParameters
                .builder()
                .resource("kontroll-device")
                .build();

        return entityConsumerFactoryService
                .createFactory(Device.class, this::processDevice)
                .createContainer(kontrolldevice);
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
