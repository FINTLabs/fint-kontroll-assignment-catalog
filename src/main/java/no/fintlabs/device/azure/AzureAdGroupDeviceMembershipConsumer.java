package no.fintlabs.device.azure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.AzureStatus;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.device.azureInfo.DeviceAzureInfoRepository;
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

    private final DeviceAzureInfoRepository deviceAzureInfoRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

   // @Bean TODO fix
    public ConcurrentMessageListenerContainer<String, AzureAdDeviceGroupMembership> azureAdDeviceMembershipConsumer(
            EventConsumerFactoryService eventConsumerFactoryService
    ) {

        return eventConsumerFactoryService.createFactory(
                        AzureAdDeviceGroupMembership.class,
                        this::processGroupMembership)
                .createContainer(EventTopicNameParameters
                        .builder()
                        .eventName("azuread-resource-device-group-membership")
                        .build());
    }

    @Async
    @Transactional
    void processGroupMembership(ConsumerRecord<String, AzureAdDeviceGroupMembership> record) {
        AzureAdDeviceGroupMembership azureResponse = record.value();
        UUID azureDeviceRef = azureResponse.getAzureDeviceRef();
        UUID azureResourceRef = azureResponse.getAzureResourceRef();
        String key = record.key();
        Optional<DeviceAzureInfo> deviceAzureInfoOptional = deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(azureDeviceRef, azureResourceRef);
        if (deviceAzureInfoOptional.isEmpty()) {
            log.warn("No deviceAzureInfo found for device {} and resource {}, messageKey: {}", azureDeviceRef, azureResourceRef, key);
        } else {
            DeviceAzureInfo deviceAzureInfo = deviceAzureInfoOptional.get();
            switch (azureResponse.getCode()) {
                case ADDED -> confirmMembershipAdded(deviceAzureInfo, key);
                case REMOVED -> confirmMembershipRemoved(deviceAzureInfo, key);
                case ERROR -> saveError(deviceAzureInfo, key);
                case FAILED -> markAsFailed(deviceAzureInfo, key);
                case NO_CHANGES ->
                        log.warn("No changes for device {} in group {}, messageKey: {}", azureResponse.getAzureDeviceRef(), azureResponse.getAzureResourceRef(), record.key());
            }
        }
    }

    private void markAsFailed(DeviceAzureInfo deviceAzureInfo, String key) {
        log.warn("Received failed status for device {} in group {}, messageKey: {}", deviceAzureInfo.getDeviceAzureId(), deviceAzureInfo.getResourceAzureId(), key);
        deviceAzureInfo.setAzureStatus(AzureStatus.NEEDS_REPUBLISH);
        deviceAzureInfoRepository.save(deviceAzureInfo);
    }

    private void saveError(DeviceAzureInfo deviceAzureInfo, String key) {
        log.warn("Received error for device {} in group {}, messageKey: {}", deviceAzureInfo.getDeviceAzureId(), deviceAzureInfo.getResourceAzureId(), key);
        deviceAzureInfo.setAzureStatus(AzureStatus.ERROR);
        deviceAzureInfoRepository.save(deviceAzureInfo);

    }

    private void confirmMembershipRemoved(DeviceAzureInfo deviceAzureInfo, String key) {
        if (deviceAzureInfo.getKontrollStatus() != KontrollStatus.INACTIVE) {
            log.info("Received confirmation for removal of device {} from group {}, messageKey: {}", deviceAzureInfo.getDeviceAzureId(), deviceAzureInfo.getResourceAzureId(), key);
            deviceAssigmentEntityProducerService.publish(deviceAzureInfo);
        } else {
            deviceAzureInfo.setAzureStatus(AzureStatus.DELETION_CONFIRMED);
            deviceAzureInfoRepository.save(deviceAzureInfo);
        }
    }

    private void confirmMembershipAdded(DeviceAzureInfo deviceAzureInfo, String key) {
        if (deviceAzureInfo.getKontrollStatus() != KontrollStatus.ACTIVE) {
            log.info("Received confirmation for addition of device {} to group {}, messageKey: {}", deviceAzureInfo.getDeviceAzureId(), deviceAzureInfo.getResourceAzureId(), key);
            deviceAssigmentEntityProducerService.publish(deviceAzureInfo);
        } else {
            deviceAzureInfo.setAzureStatus(AzureStatus.CONFIRMED);
            deviceAzureInfoRepository.save(deviceAzureInfo);
        }
    }

}
