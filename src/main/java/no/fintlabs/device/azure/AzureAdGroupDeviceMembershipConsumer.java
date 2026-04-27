package no.fintlabs.device.azure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.entraInfo.DeviceEntraInfo;
import no.fintlabs.device.entraInfo.DeviceEntraInfoRepository;
import no.fintlabs.kafka.event.EventConsumerFactoryService;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AzureAdGroupDeviceMembershipConsumer {

    private final DeviceEntraInfoRepository deviceEntraInfoRepository;
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

    @Async
    @Transactional
    void processGroupMembership(ConsumerRecord<String, EntraDeviceGroupMembership> record) {
        EntraDeviceGroupMembership entraResponse = record.value();
        UUID azureDeviceRef = entraResponse.getEntraDeviceRef();
        UUID azureResourceRef = entraResponse.getEntraResourceRef();
        String key = record.key();
        Optional<DeviceEntraInfo> deviceAzureInfoOptional = deviceEntraInfoRepository.findByDeviceAzureIdAndResourceAzureId(azureDeviceRef, azureResourceRef);
        if (deviceAzureInfoOptional.isEmpty()) {
            log.warn("No deviceAzureInfo found for device {} and resource {}, messageKey: {}", azureDeviceRef, azureResourceRef, key);
        } else {
            DeviceEntraInfo deviceEntraInfo = deviceAzureInfoOptional.get();
            log.info("Received response for device {} in group {}, messageKey: {}", azureDeviceRef, azureResourceRef, key);
            switch (entraResponse.getCode()) {
                case ADDED -> confirmMembershipAdded(deviceEntraInfo, key);
                case REMOVED -> confirmMembershipRemoved(deviceEntraInfo, key);
                case ERROR -> saveError(deviceEntraInfo, key);
                case FAILED -> markAsFailed(deviceEntraInfo, key);
                case NO_CHANGES ->
                        log.warn("No changes for device {} in group {}, messageKey: {}", entraResponse.getEntraDeviceRef(), entraResponse.getEntraResourceRef(), record.key());
            }
        }
    }

    private void markAsFailed(DeviceEntraInfo deviceEntraInfo, String key) {
        log.warn("Received failed status for device {} in group {}, messageKey: {}", deviceEntraInfo.getDeviceAzureId(), deviceEntraInfo.getResourceAzureId(), key);
        deviceEntraInfo.setEntraStatus(EntraStatus.NEEDS_REPUBLISH);
        deviceEntraInfoRepository.save(deviceEntraInfo);
    }

    private void saveError(DeviceEntraInfo deviceEntraInfo, String key) {
        log.warn("Received error for device {} in group {}, messageKey: {}", deviceEntraInfo.getDeviceAzureId(), deviceEntraInfo.getResourceAzureId(), key);
        deviceEntraInfo.setEntraStatus(EntraStatus.ERROR);
        deviceEntraInfoRepository.save(deviceEntraInfo);

    }

    private void confirmMembershipRemoved(DeviceEntraInfo deviceEntraInfo, String key) {
        if (deviceEntraInfo.getKontrollStatus() != KontrollStatus.INACTIVE) {
            log.info("Received confirmation for removal of device {} from group {}, messageKey: {}", deviceEntraInfo.getDeviceAzureId(), deviceEntraInfo.getResourceAzureId(), key);
            deviceAssigmentEntityProducerService.publish(deviceEntraInfo);
        } else {
            deviceEntraInfo.setEntraStatus(EntraStatus.DELETION_CONFIRMED);
            deviceEntraInfoRepository.save(deviceEntraInfo);
        }
    }

    private void confirmMembershipAdded(DeviceEntraInfo deviceEntraInfo, String key) {
        if (deviceEntraInfo.getKontrollStatus() != KontrollStatus.ACTIVE) {
            log.info("Received confirmation for addition of device {} to group {}, messageKey: {}", deviceEntraInfo.getDeviceAzureId(), deviceEntraInfo.getResourceAzureId(), key);
            deviceAssigmentEntityProducerService.publish(deviceEntraInfo);
        } else {
            deviceEntraInfo.setEntraStatus(EntraStatus.CONFIRMED);
            deviceEntraInfoRepository.save(deviceEntraInfo);
        }
    }

}
