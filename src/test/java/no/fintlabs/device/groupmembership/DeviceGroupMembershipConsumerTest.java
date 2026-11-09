package no.fintlabs.device.groupmembership;

import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.common.RetryConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DeviceGroupMembershipConsumerTest {

    @Test
    void failedRecordStopsAfterThreeAttemptsAndNextRecordCanBeProcessed() {
        DeviceGroupMembershipService service = mock(DeviceGroupMembershipService.class);
        RetryTemplate retryTemplate = new RetryConfig().kafkaRetryTemplate();
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        DeviceGroupMembershipConsumer consumer = new DeviceGroupMembershipConsumer(
                service, retryTemplate, mock(KafkaConsumerConfigurationDefaults.class));

        DeviceGroupMembership missingDevice = DeviceGroupMembership.builder()
                .deviceId(3L).deviceGroupId(1L).build();
        DeviceGroupMembership validMembership = DeviceGroupMembership.builder()
                .deviceId(4L).deviceGroupId(1L).build();
        doThrow(new IllegalArgumentException("Device not found: 3"))
                .when(service).saveOrUpdate(missingDevice);

        assertThatThrownBy(() -> consumer.processWithRetry(record("3_1", missingDevice)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(service, times(RetryConfig.KAFKA_RETRY_MAX_ATTEMPTS)).saveOrUpdate(missingDevice);

        consumer.processWithRetry(record("4_1", validMembership));
        verify(service).saveOrUpdate(validMembership);
    }

    private ConsumerRecord<String, DeviceGroupMembership> record(String key, DeviceGroupMembership membership) {
        return new ConsumerRecord<>("kontroll-device-group-membership", 0, 0L, key, membership);
    }
}
