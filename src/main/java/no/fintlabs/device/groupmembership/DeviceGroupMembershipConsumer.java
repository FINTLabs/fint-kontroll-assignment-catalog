package no.fintlabs.device.groupmembership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
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

    @Bean
    public ConcurrentMessageListenerContainer<String, DeviceGroupMembership> deviceGroupMembershipConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters deviceGroupMembership = EntityTopicNameParameters
                .builder()
                .resource("kontroll-device-group-membership")
                .build();

        return entityConsumerFactoryService
                .createFactory(DeviceGroupMembership.class, this::processWithRetry)
                .createContainer(deviceGroupMembership);
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
