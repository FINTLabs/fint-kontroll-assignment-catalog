package no.fintlabs.assignment.device.entra;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.KafkaTopics;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.entra.membership.event.MembershipEventNames;
import no.fintlabs.entra.membership.event.OperationType;
import no.fintlabs.entra.membership.event.DeviceResourceGroupMembershipCommand;
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

    private final ParameterizedTemplate<DeviceResourceGroupMembershipCommand> membershipProducer;
    private final EventTopicNameParameters resourceGroupMembershipTopicNameParameters;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    public DeviceEntraMembershipPublisher(
            ParameterizedTemplateFactory producerFactory,
            EventTopicService entityTopicService,
            DeviceEntraMembershipRepository deviceEntraMembershipRepository
    ) {
        membershipProducer = producerFactory.createTemplate(DeviceResourceGroupMembershipCommand.class);
        this.deviceEntraMembershipRepository = deviceEntraMembershipRepository;

        resourceGroupMembershipTopicNameParameters = KafkaTopics.eventTopicNameParameters(MembershipEventNames.RESOURCE_GROUP_MEMBERSHIP_DEVICE);
        entityTopicService.createOrModifyTopic(
                resourceGroupMembershipTopicNameParameters,
                KafkaTopics.eventTopicConfiguration()
        );
    }

    public void publish(DeviceEntraMembership deviceEntraMembership) {
        publish(deviceEntraMembership, false);
    }

    public void publish(DeviceEntraMembership deviceEntraMembership, boolean force) {
        if (!isValidMembership(deviceEntraMembership)) {
            return;
        }

        if (EntraMembershipSyncStatus.ERROR.equals(deviceEntraMembership.getEntraStatus()) && !force) {
            log.warn("DeviceEntraMembership with id {} has EntraMembershipSyncStatus ERROR. Skipping publishing to Entra.", deviceEntraMembership.getId());
            return;
        }
        if (EntraMembershipSyncStatus.ERROR.equals(deviceEntraMembership.getEntraStatus())) {
            log.warn("DeviceEntraMembership with id {} has EntraMembershipSyncStatus ERROR. Force publishing to Entra.", deviceEntraMembership.getId());
        }
        log.info("Publishing to Entra - device Entra membership with id: {}", deviceEntraMembership.getId());
        if (MembershipStatus.ACTIVE.equals(deviceEntraMembership.getMembershipStatus())) {
            deviceEntraMembership.setEntraStatus(EntraMembershipSyncStatus.SENT);
            deviceEntraMembership.setSentToEntraAt(new Date());
            deviceEntraMembershipRepository.save(deviceEntraMembership);
            publishMembershipCommand(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.ADD);
        } else {
            deviceEntraMembership.setEntraStatus(EntraMembershipSyncStatus.DELETION_SENT);
            deviceEntraMembership.setDeletionSentToEntraAt(new Date());
            deviceEntraMembershipRepository.save(deviceEntraMembership);
            publishMembershipCommand(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId(), OperationType.REMOVE);
        }
    }

    private boolean isValidMembership(DeviceEntraMembership deviceEntraMembership) {
        if (!hasValidEntraIds(deviceEntraMembership.getResourceEntraId(), deviceEntraMembership.getDeviceEntraId())) {
            log.warn("Publishing device Entra membership skipped for membership id {}. Missing Entra group id ({}) or device id ({})",
                    deviceEntraMembership.getId(),
                    deviceEntraMembership.getResourceEntraId(),
                    deviceEntraMembership.getDeviceEntraId());
            return false;
        }
        return true;
    }

    private boolean hasValidEntraIds(UUID entraGroupId, UUID entraDeviceId) {
        return entraGroupId != null && entraDeviceId != null;
    }

    private void publishMembershipCommand(UUID entraGroupId, UUID entraDeviceId, OperationType operationType) {
        DeviceResourceGroupMembershipCommand membershipCommand = new DeviceResourceGroupMembershipCommand(operationType, entraGroupId, entraDeviceId);

        log.info("Publishing {} to Entra - groupId: {}, deviceId: {}", operationType, entraGroupId, entraDeviceId);

        send(membershipCommand);
    }

    private void send(DeviceResourceGroupMembershipCommand membershipCommand) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNow(membershipCommand);
                }
            });
            return;
        }

        sendNow(membershipCommand);
    }

    private void sendNow(DeviceResourceGroupMembershipCommand membershipCommand) {
        String key = System.currentTimeMillis() + "-" + RandomStringUtils.randomAlphanumeric(6);
        membershipProducer.send(
                ParameterizedProducerRecord.<DeviceResourceGroupMembershipCommand>builder()
                        .topicNameParameters(resourceGroupMembershipTopicNameParameters)
                        .key(key)
                        .value(membershipCommand)
                        .build()
        );
    }


}
