package no.fintlabs.device;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class DeviceAssigmentEntityProducerService {

    private final EventProducer<DeviceResourceGroupMembership> entityProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;

    public DeviceAssigmentEntityProducerService(
            EventProducerFactory entityProducerFactory,
            EventTopicService entityTopicService
    ) {
        entityProducer = entityProducerFactory.createProducer(DeviceResourceGroupMembership.class);

        resourceGroupMembershipTopicNameParameters = EventTopicNameParameters
                .builder()
                .eventName("azure-resource-group-membership-device")
                .build();
        // TODO set it up with correct values
        entityTopicService.ensureTopic(resourceGroupMembershipTopicNameParameters, 0);
    }

    public void publish(DeviceAzureInfo deviceAzureInfo) {
        if (deviceAzureInfo.getAzureStatus().equals(AzureStatus.ERROR)) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Skipping publishing to Azure.", deviceAzureInfo.getId());
        }
        if (deviceAzureInfo.getKontrollStatus().equals(KontrollStatus.ACTIVE)) {
            publish(deviceAzureInfo.getResourceAzureId(), deviceAzureInfo.getDeviceAzureId(), OperationType.ADD);
            deviceAzureInfo.setAzureStatus(AzureStatus.SENT);
            deviceAzureInfo.setSentToAzureAt(new Date());
        } else {
            publish(deviceAzureInfo.getResourceAzureId(), deviceAzureInfo.getDeviceAzureId(), OperationType.REMOVE);
            deviceAzureInfo.setAzureStatus(AzureStatus.DELETION_SENT);
            deviceAzureInfo.setDeletionSentToAzureAt(new Date());
        }
    }

    private void publish(UUID azureAdGroupId, UUID azureDeviceId, OperationType action) {
        String key = Instant.now().toString();
        DeviceResourceGroupMembership azureAdGroupMembership = new DeviceResourceGroupMembership(action, azureAdGroupId, azureDeviceId);

        log.info("Publishing to Azure - groupId: {}, deviceId: {}, action: {}", azureAdGroupId, azureDeviceId, action);

        entityProducer.send(
                EventProducerRecord.<DeviceResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }


}
