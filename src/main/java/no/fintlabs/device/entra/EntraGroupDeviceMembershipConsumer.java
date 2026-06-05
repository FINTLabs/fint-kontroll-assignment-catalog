package no.fintlabs.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.groupmembership.MembershipEventNames;
import no.fintlabs.kafka.KafkaEventTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EntraGroupDeviceMembershipConsumer {

    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraDeviceGroupMembership> entraDeviceMembershipConsumer(
            ParameterizedListenerContainerFactoryService eventConsumerFactoryService
    ) {

        return eventConsumerFactoryService.createRecordListenerContainerFactory(
                        EntraDeviceGroupMembership.class,
                        this::processGroupMembership,
                        KafkaEventTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEventTopics.topicNameParameters(MembershipEventNames.ENTRA_DEVICE_GROUP_MEMBERSHIP));
    }

    @Transactional
    public void processGroupMembership(ConsumerRecord<String, EntraDeviceGroupMembership> record) {
        EntraDeviceGroupMembership entraResponse = record.value();
        UUID entraDeviceRef = entraResponse.getEntraDeviceRef();
        UUID entraGroupRef = entraResponse.getEntraGroupRef();
        String key = record.key();
        Optional<DeviceEntraMembership> deviceEntraMembershipOptional = deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(entraDeviceRef, entraGroupRef);
        if (deviceEntraMembershipOptional.isEmpty()) {
            log.warn("No device Entra membership found for device {} and resource {}, messageKey: {}", entraDeviceRef, entraGroupRef, key);
        } else {
            DeviceEntraMembership deviceEntraMembership = deviceEntraMembershipOptional.get();
            log.info("Received response for device {} in group {}, messageKey: {}", entraDeviceRef, entraGroupRef, key);
            switch (entraResponse.getCode()) {
                case ADDED -> confirmMembershipAdded(deviceEntraMembership, key);
                case REMOVED -> confirmMembershipRemoved(deviceEntraMembership, key);
                case ERROR -> markAsError(deviceEntraMembership, key);
                case FAILED -> markAsFailed(deviceEntraMembership, key);
                case NO_CHANGES -> handleNoChangesResponse(deviceEntraMembership, key);

            }
            deviceEntraMembershipRepository.save(deviceEntraMembership);
        }
    }

    private void handleNoChangesResponse(DeviceEntraMembership deviceEntraMembership, String key) {
        log.info("Received no changes response for device {} in group {}, messageKey: {}", deviceEntraMembership.getDeviceEntraId(), deviceEntraMembership.getResourceEntraId(), key);
        if (deviceEntraMembership.getMembershipStatus() == MembershipStatus.ACTIVE) {
            deviceEntraMembership.setEntraStatus(EntraStatus.MEMBERSHIP_CONFIRMED);
        } else if (deviceEntraMembership.getMembershipStatus() == MembershipStatus.INACTIVE) {
            deviceEntraMembership.setEntraStatus(EntraStatus.DELETION_CONFIRMED);
        }
    }

    private void markAsFailed(DeviceEntraMembership deviceEntraMembership, String key) {
        log.warn("Received failed status for device {} in group {}, messageKey: {}", deviceEntraMembership.getDeviceEntraId(), deviceEntraMembership.getResourceEntraId(), key);
        deviceEntraMembership.setEntraStatus(EntraStatus.NEEDS_REPUBLISH);
    }

    private void markAsError(DeviceEntraMembership deviceEntraMembership, String key) {
        log.warn("Received error for device {} in group {}, messageKey: {}", deviceEntraMembership.getDeviceEntraId(), deviceEntraMembership.getResourceEntraId(), key);
        deviceEntraMembership.setEntraStatus(EntraStatus.ERROR);
    }

    private void confirmMembershipRemoved(DeviceEntraMembership deviceEntraMembership, String key) {
        if (deviceEntraMembership.getMembershipStatus() != MembershipStatus.INACTIVE) {
            log.info("Received confirmation for removal of device {} from group {}, messageKey: {}", deviceEntraMembership.getDeviceEntraId(), deviceEntraMembership.getResourceEntraId(), key);
            deviceAssigmentEntityProducerService.publish(deviceEntraMembership, true);
        } else {
            deviceEntraMembership.setEntraStatus(EntraStatus.DELETION_CONFIRMED);
        }
    }

    private void confirmMembershipAdded(DeviceEntraMembership deviceEntraMembership, String key) {
        if (deviceEntraMembership.getMembershipStatus() != MembershipStatus.ACTIVE) {
            log.info("Received confirmation for addition of device {} to group {}, messageKey: {}", deviceEntraMembership.getDeviceEntraId(), deviceEntraMembership.getResourceEntraId(), key);
            deviceAssigmentEntityProducerService.publish(deviceEntraMembership, true);
        } else {
            deviceEntraMembership.setEntraStatus(EntraStatus.MEMBERSHIP_CONFIRMED);
        }
    }
}
