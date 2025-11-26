package no.fintlabs.device.group;

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
public class DeviceGroupConsumer {

    private final DeviceGroupService deviceGroupService;

    @Bean
    public ConcurrentMessageListenerContainer<String, DeviceGroup> deviceGroupConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters kontrolldevice = EntityTopicNameParameters
                .builder()
                .resource("kontroll-device-group")
                .build();

        return entityConsumerFactoryService
                .createFactory(DeviceGroup.class, this::processDeviceGroup)
                .createContainer(kontrolldevice);
    }

    private void processDeviceGroup(ConsumerRecord<String, DeviceGroup> stringDeviceGroupConsumerRecord) {
        DeviceGroup incomingDevice = stringDeviceGroupConsumerRecord.value();
        if (incomingDevice != null) {
            deviceGroupService.saveOrUpdate(incomingDevice);
        }
    }


}
