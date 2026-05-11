package no.fintlabs.device;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.entra.DeviceEntraMembership;
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

    public void publish(DeviceEntraMembership deviceEntraMembership) {
        if (deviceEntraMembership.getEntraStatus().equals(EntraStatus.ERROR)) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Skipping publishing to Azure.", deviceEntraMembership.getId());
        }
        log.info("Publishing to Azure - deviceEntraInfo: {}", deviceEntraMembership);
        if (deviceEntraMembership.getKontrollStatus().equals(KontrollStatus.ACTIVE)) {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.ADD);
            deviceEntraMembership.setEntraStatus(EntraStatus.SENT);
            deviceEntraMembership.setSentToAzureAt(new Date());
        } else {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.REMOVE);
            deviceEntraMembership.setEntraStatus(EntraStatus.DELETION_SENT);
            deviceEntraMembership.setDeletionSentToAzureAt(new Date());
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
