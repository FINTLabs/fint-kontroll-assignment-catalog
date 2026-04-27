package no.fintlabs.device;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.entraInfo.DeviceEntraInfo;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerFactory;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.topic.EventTopicNameParameters;
import no.fintlabs.kafka.event.topic.EventTopicService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class DeviceAssigmentEntityProducerService {

    private final EventProducer<DeviceResourceGroupMembership> eventProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;

    public DeviceAssigmentEntityProducerService(
            EventProducerFactory entityProducerFactory,
            EventTopicService entityTopicService
    ) {
        eventProducer = entityProducerFactory.createProducer(DeviceResourceGroupMembership.class);

        resourceGroupMembershipTopicNameParameters = EventTopicNameParameters
                .builder()
                .eventName("kontroll-resource-group-membership-device")
                .build();
        // TODO set it up with correct values
        entityTopicService.ensureTopic(resourceGroupMembershipTopicNameParameters, 0);
    }

    public void publish(DeviceEntraInfo deviceEntraInfo) {
        if (deviceEntraInfo.getEntraStatus().equals(EntraStatus.ERROR)) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Skipping publishing to Azure.", deviceEntraInfo.getId());
        }
        log.info("Publishing to Azure - deviceEntraInfo: {}", deviceEntraInfo);
        if (deviceEntraInfo.getKontrollStatus().equals(KontrollStatus.ACTIVE)) {
            publish(deviceEntraInfo.getResourceAzureId(), deviceEntraInfo.getDeviceAzureId(), OperationType.ADD);
            deviceEntraInfo.setEntraStatus(EntraStatus.SENT);
            deviceEntraInfo.setSentToAzureAt(new Date());
        } else {
            publish(deviceEntraInfo.getResourceAzureId(), deviceEntraInfo.getDeviceAzureId(), OperationType.REMOVE);
            deviceEntraInfo.setEntraStatus(EntraStatus.DELETION_SENT);
            deviceEntraInfo.setDeletionSentToAzureAt(new Date());
        }
    }

    private void publish(UUID azureAdGroupId, UUID azureDeviceId, OperationType action) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        DeviceResourceGroupMembership azureAdGroupMembership = new DeviceResourceGroupMembership(action, azureAdGroupId, azureDeviceId);

        log.info("Publishing to Azure - groupId: {}, deviceId: {}, action: {}", azureAdGroupId, azureDeviceId, action);

        eventProducer.send(
                EventProducerRecord.<DeviceResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }


}
