package no.fintlabs.assignment.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.entra.EntraMembershipBatchLookup;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntraGroupDeviceMembershipProcessor {

    private final DeviceEntraMembershipRepository repository;
    private final DeviceEntraMembershipPublisher publisher;
    private final EntraMembershipBatchLookup batchLookup;

    public void processBatch(List<ConsumerRecord<String, EntraDeviceGroupMembership>> records) {
        Set<EntraMembershipBatchLookup.MembershipKey> requested = new LinkedHashSet<>();
        for (ConsumerRecord<String, EntraDeviceGroupMembership> record : records) {
            EntraDeviceGroupMembership response = record.value();
            if (response != null && response.getCode() != null
                    && response.getEntraDeviceRef() != null && response.getEntraGroupRef() != null) {
                requested.add(new EntraMembershipBatchLookup.MembershipKey(
                        response.getEntraDeviceRef(), response.getEntraGroupRef()));
            }
        }

        if (requested.isEmpty()) {
            for (ConsumerRecord<String, EntraDeviceGroupMembership> record : records) {
                logInvalid(record);
            }
            log.info("Processed Entra device membership batch: received={}, matched=0", records.size());
            return;
        }

        Map<EntraMembershipBatchLookup.MembershipKey, DeviceEntraMembership> memberships = new HashMap<>();
        boolean useIndividualLookup = false;
        try {
            batchLookup.findAll(requested, EntraMembershipBatchLookup.MembershipTable.DEVICE, repository)
                    .forEach(membership -> memberships.put(
                            new EntraMembershipBatchLookup.MembershipKey(
                                    membership.getDeviceEntraId(), membership.getResourceEntraId()), membership));
        } catch (RuntimeException exception) {
            log.error("Batch lookup failed; looking up device Entra memberships individually", exception);
            useIndividualLookup = true;
        }

        Set<DeviceEntraMembership> changed = new LinkedHashSet<>();
        int matchedRecords = 0;
        int failedRecords = 0;
        for (ConsumerRecord<String, EntraDeviceGroupMembership> record : records) {
            try {
                EntraDeviceGroupMembership response = record.value();
                if (response == null || response.getCode() == null
                        || response.getEntraDeviceRef() == null || response.getEntraGroupRef() == null) {
                    logInvalid(record);
                    continue;
                }

                DeviceEntraMembership membership = useIndividualLookup
                        ? repository.findByDeviceEntraIdAndResourceEntraId(
                                response.getEntraDeviceRef(), response.getEntraGroupRef()).orElse(null)
                        : memberships.get(new EntraMembershipBatchLookup.MembershipKey(
                                response.getEntraDeviceRef(), response.getEntraGroupRef()));
                if (membership == null) {
                    log.warn("No device Entra membership found for device {} and resource {}, messageKey: {}",
                            response.getEntraDeviceRef(), response.getEntraGroupRef(), record.key());
                    continue;
                }
                matchedRecords++;

                log.debug("Received response for device {} in group {}, messageKey: {}",
                        response.getEntraDeviceRef(), response.getEntraGroupRef(), record.key());
                switch (response.getCode()) {
                    case ADDED -> confirmMembershipAdded(membership, record.key());
                    case ALREADY_PRESENT -> {
                        log.debug("Device {} was already present in Entra group {} when ADD was requested, messageKey: {}",
                                response.getEntraDeviceRef(), response.getEntraGroupRef(), record.key());
                        confirmMembershipAdded(membership, record.key());
                    }
                    case REMOVED -> confirmMembershipRemoved(membership, record.key());
                    case ALREADY_REMOVED -> {
                        log.debug("Device {} was already absent from Entra group {} when REMOVE was requested, messageKey: {}",
                                response.getEntraDeviceRef(), response.getEntraGroupRef(), record.key());
                        confirmMembershipRemoved(membership, record.key());
                    }
                    case ERROR -> {
                        log.warn("Received error for device {} in group {}, messageKey: {}",
                                membership.getDeviceEntraId(), membership.getResourceEntraId(), record.key());
                        membership.setEntraStatus(EntraMembershipSyncStatus.ERROR);
                    }
                    case FAILED -> {
                        log.warn("Received failed status for device {} in group {}, messageKey: {}",
                                membership.getDeviceEntraId(), membership.getResourceEntraId(), record.key());
                        membership.setEntraStatus(EntraMembershipSyncStatus.NEEDS_REPUBLISH);
                    }
                }
                changed.add(membership);
            } catch (RuntimeException exception) {
                failedRecords++;
                log.error("Failed to process device Entra membership result. topic={}, partition={}, offset={}, key={}",
                        record.topic(), record.partition(), record.offset(), record.key(), exception);
            }
        }
        int failedSaves = 0;
        if (!changed.isEmpty()) {
            try {
                repository.saveAll(changed);
            } catch (RuntimeException exception) {
                log.error("Batch save failed; saving {} device Entra memberships individually", changed.size(), exception);
                for (DeviceEntraMembership membership : changed) {
                    try {
                        repository.save(membership);
                    } catch (RuntimeException recordFailure) {
                        failedSaves++;
                        log.error("Failed to save device Entra membership. deviceId={}, groupId={}",
                                membership.getDeviceEntraId(), membership.getResourceEntraId(), recordFailure);
                    }
                }
            }
        }
        if (failedRecords > 0 || failedSaves > 0) {
            log.error("Entra device membership batch completed with failures: received={}, failedRecords={}, failedSaves={}. Failed results were logged and skipped.",
                    records.size(), failedRecords, failedSaves);
        } else {
            log.info("Processed Entra device membership batch: received={}, matched={}, saved={}",
                    records.size(), matchedRecords, changed.size());
        }
    }

    private void logInvalid(ConsumerRecord<String, EntraDeviceGroupMembership> record) {
        if (record.value() == null) {
            log.warn("Skipping null Entra device membership result for key {}", record.key());
        } else {
            log.warn("Skipping Entra device membership result with missing code or device/group ID for key {}", record.key());
        }
    }

    private void confirmMembershipRemoved(DeviceEntraMembership membership, String key) {
        if (membership.getMembershipStatus() != MembershipStatus.INACTIVE) {
            log.debug("Received confirmation for removal of device {} from group {}, messageKey: {}",
                    membership.getDeviceEntraId(), membership.getResourceEntraId(), key);
            publisher.publish(membership, true);
        } else {
            membership.setEntraStatus(EntraMembershipSyncStatus.DELETION_CONFIRMED);
        }
    }

    private void confirmMembershipAdded(DeviceEntraMembership membership, String key) {
        if (membership.getMembershipStatus() != MembershipStatus.ACTIVE) {
            log.debug("Received confirmation for addition of device {} to group {}, messageKey: {}",
                    membership.getDeviceEntraId(), membership.getResourceEntraId(), key);
            publisher.publish(membership, true);
        } else {
            membership.setEntraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        }
    }

}
