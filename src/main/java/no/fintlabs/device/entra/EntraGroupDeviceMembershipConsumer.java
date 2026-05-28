package no.fintlabs.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.MembershipStatus;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
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

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraDeviceGroupMembership> azureAdDeviceMembershipConsumer(
            EventConsumerFactoryService eventConsumerFactoryService
    ) {

        return eventConsumerFactoryService.createFactory(
                        EntraDeviceGroupMembership.class,
                        this::processGroupMembership)
                .createContainer(EventTopicNameParameters
                        .builder()
                        .eventName("entra-device-group-membership")
                        .build());
    }

    @Transactional
    public void processGroupMembership(ConsumerRecord<String, EntraDeviceGroupMembership> record) {
        EntraDeviceGroupMembership entraResponse = record.value();
        UUID azureDeviceRef = entraResponse.getEntraDeviceRef();
        UUID entraGroupRef = entraResponse.getEntraGroupRef();
        String key = record.key();
        Optional<DeviceEntraMembership> deviceEntraMembershipOptional = deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(azureDeviceRef, entraGroupRef);
        if (deviceEntraMembershipOptional.isEmpty()) {
            log.warn("No deviceAzureInfo found for device {} and resource {}, messageKey: {}", azureDeviceRef, entraGroupRef, key);
        } else {
            DeviceEntraMembership deviceEntraMembership = deviceEntraMembershipOptional.get();
            log.info("Received response for device {} in group {}, messageKey: {}", azureDeviceRef, entraGroupRef, key);
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
