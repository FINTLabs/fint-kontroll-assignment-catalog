package no.fintlabs.assignment.device.entra;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.kafka.KafkaTopics;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.entra.membership.event.MembershipEventNames;
import no.fintlabs.entra.membership.event.OperationType;
import no.fintlabs.entra.membership.event.DeviceResourceGroupMembershipCommand;
import no.fintlabs.kafka.KafkaEventTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EventTopicService;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class DeviceEntraMembershipPublisher {

    private final ParameterizedTemplate<DeviceResourceGroupMembershipCommand> eventProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    public DeviceEntraMembershipPublisher(
            ParameterizedTemplateFactory entityProducerFactory,
            EventTopicService entityTopicService,
            DeviceEntraMembershipRepository deviceEntraMembershipRepository
    ) {
        eventProducer = entityProducerFactory.createTemplate(DeviceResourceGroupMembershipCommand.class);
        this.deviceEntraMembershipRepository = deviceEntraMembershipRepository;

        resourceGroupMembershipTopicNameParameters = KafkaTopics.eventTopicNameParameters("resource-group-membership-device");
        entityTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaTopics.eventTopicConfiguration()
        );
    }

    public void publish(DeviceEntraMembership deviceEntraMembership, boolean force) {
        if (EntraMembershipSyncStatus.ERROR.equals(deviceEntraMembership.getEntraStatus()) && !force) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Skipping publishing to Azure.", deviceEntraMembership.getId());
            return;
        }
        if (EntraMembershipSyncStatus.ERROR.equals(deviceEntraMembership.getEntraStatus())) {
            log.warn("DeviceAzureInfo with id {} has AzureStatus ERROR. Force publishing to Azure.", deviceEntraMembership.getId());
        }
        log.info("Publishing to Azure - deviceEntraInfo with id: {}", deviceEntraMembership.getId());
        if (MembershipStatus.ACTIVE.equals(deviceEntraMembership.getMembershipStatus())) {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.ADD);
            deviceEntraMembership.setEntraStatus(EntraMembershipSyncStatus.SENT);
            deviceEntraMembership.setSentToEntraAt(new Date());
        } else {
            publish(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.REMOVE);
            deviceEntraMembership.setEntraStatus(EntraMembershipSyncStatus.DELETION_SENT);
            deviceEntraMembership.setDeletionSentToEntraAt(new Date());
        }
        deviceEntraMembershipRepository.save(deviceEntraMembership);
    }

    private void publish(UUID azureAdGroupId, UUID azureDeviceId, OperationType action) {
        DeviceResourceGroupMembershipCommand azureAdGroupMembership = new DeviceResourceGroupMembershipCommand(action, azureAdGroupId, azureDeviceId);

        log.info("Publishing to Azure - groupId: {}, deviceId: {}, action: {}", azureAdGroupId, azureDeviceId, action);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(azureAdGroupMembership);
                }
            });
            return;
        }

        send(azureAdGroupMembership);
    }

    private void send(DeviceResourceGroupMembershipCommand azureAdGroupMembership) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        eventProducer.send(
                ParameterizedProducerRecord.<DeviceResourceGroupMembershipCommand>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }


}
