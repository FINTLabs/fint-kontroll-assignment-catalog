package no.fintlabs.device;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.groupmembership.MembershipEventNames;
import no.fintlabs.groupmembership.OperationType;
import no.fintlabs.kafka.KafkaEventTopics;
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

        resourceGroupMembershipTopicNameParameters = KafkaEventTopics.topicNameParameters(MembershipEventNames.RESOURCE_GROUP_MEMBERSHIP_DEVICE);
        entityTopicService.createOrModifyTopic(resourceGroupMembershipTopicNameParameters, KafkaEventTopics.topicConfiguration());
    }

    public void publish(DeviceEntraMembership deviceEntraMembership, boolean force) {
        if (deviceEntraMembership.getEntraStatus().equals(EntraStatus.ERROR) || force) {
            log.warn("deviceEntraMembership with id {} has EntraStatus ERROR. Skipping publishing to Entra.", deviceEntraMembership.getId());
        }
        log.info("Publishing to Entra - deviceEntraInfo with id: {}", deviceEntraMembership.getId());
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

    private void publish(UUID entraIdGroupId, UUID entraDeviceId, OperationType action) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        DeviceResourceGroupMembership deviceGroupMembership = new DeviceResourceGroupMembership(action, entraIdGroupId, entraDeviceId);

        log.info("Publishing to Entra - groupId: {}, deviceId: {}, action: {}", entraIdGroupId, entraDeviceId, action);

        eventProducer.send(
                ParameterizedProducerRecord.<DeviceResourceGroupMembership>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(deviceGroupMembership)
                        .build()
        );
    }


}
