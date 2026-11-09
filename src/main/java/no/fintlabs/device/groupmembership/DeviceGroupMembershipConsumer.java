package no.fintlabs.device.groupmembership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import static no.fintlabs.common.RetryConfig.KAFKA_RETRY_MAX_ATTEMPTS;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceGroupMembershipConsumer {

    private static final int MAX_POLL_RECORDS = 10;

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
                        KafkaTopics.listenerConfigurationWithMaxPollRecords(MAX_POLL_RECORDS),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaTopics.topicNameParameters("kontroll-device-group-membership"));
    }
    void processWithRetry(ConsumerRecord<String, DeviceGroupMembership> record) {
        try {
            kafkaRetryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    log.warn(
                            "Retrying device group membership processing. attempt={} of {}, key={}, deviceId={}, deviceGroupId={}, reason={}",
                            context.getRetryCount() + 1,
                            KAFKA_RETRY_MAX_ATTEMPTS,
                            record.key(),
                            getDeviceId(record),
                            getDeviceGroupId(record),
                            context.getLastThrowable().getMessage()
                    );
                }
                processDeviceGroupMembership(record);
                return null;
            });
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to process device group membership after {} attempts. topic={}, partition={}, offset={}, key={}, deviceId={}, deviceGroupId={}. Passing record to Kafka skip handler.",
                    KAFKA_RETRY_MAX_ATTEMPTS,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    getDeviceId(record),
                    getDeviceGroupId(record),
                    exception
            );
            throw exception;
        }
    }
    private void processDeviceGroupMembership(ConsumerRecord<String, DeviceGroupMembership> stringDeviceGroupMembershipConsumerRecord) {
        DeviceGroupMembership incomingMembership = stringDeviceGroupMembershipConsumerRecord.value();

        if (incomingMembership != null) {
            deviceGroupMembershipService.saveOrUpdate(incomingMembership);
        }
    }

    private Long getDeviceId(ConsumerRecord<String, DeviceGroupMembership> record) {
        return record.value() == null ? null : record.value().getDeviceId();
    }

    private Long getDeviceGroupId(ConsumerRecord<String, DeviceGroupMembership> record) {
        return record.value() == null ? null : record.value().getDeviceGroupId();
    }

}
