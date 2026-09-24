package no.fintlabs.device;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class DeviceAssigmentEntityProducerService {

    private final ParameterizedTemplate<DeviceResourceGroupMembership> eventProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    public DeviceAssigmentEntityProducerService(
            ParameterizedTemplateFactory entityProducerFactory,
            EventTopicService entityTopicService,
            DeviceEntraMembershipRepository deviceEntraMembershipRepository
    ) {
        eventProducer = entityProducerFactory.createTemplate(DeviceResourceGroupMembership.class);
        this.deviceEntraMembershipRepository = deviceEntraMembershipRepository;

        resourceGroupMembershipTopicNameParameters = KafkaEntityTopics.eventTopicNameParameters("resource-group-membership-device");
        entityTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaEntityTopics.eventTopicConfiguration()
        );
    }

    public void publish(DeviceEntraMembership deviceEntraMembership, boolean force) {
        if (deviceEntraMembership.getEntraStatus().equals(EntraStatus.ERROR) || force) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Skipping publishing to Azure.", deviceEntraMembership.getId());
        }
        log.info("Publishing to Azure - deviceEntraInfo with id: {}", deviceEntraMembership.getId());
        if (deviceEntraMembership.getMembershipStatus().equals(MembershipStatus.ACTIVE)) {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.ADD);
            deviceEntraMembership.setEntraStatus(EntraStatus.SENT);
            deviceEntraMembership.setSentToEntraAt(new Date());
        } else {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.REMOVE);
            deviceEntraMembership.setEntraStatus(EntraStatus.DELETION_SENT);
            deviceEntraMembership.setDeletionSentToEntraAt(new Date());
        }
        deviceEntraMembershipRepository.save(deviceEntraMembership);
    }

    private void publish(UUID azureAdGroupId, UUID azureDeviceId, OperationType action) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        DeviceResourceGroupMembership azureAdGroupMembership = new DeviceResourceGroupMembership(action, azureAdGroupId, azureDeviceId);

        log.info("Publishing to Azure - groupId: {}, deviceId: {}, action: {}", azureAdGroupId, azureDeviceId, action);

        eventProducer.send(
                ParameterizedProducerRecord.<DeviceResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }


}
