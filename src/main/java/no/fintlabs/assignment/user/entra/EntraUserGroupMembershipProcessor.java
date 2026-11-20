package no.fintlabs.assignment.user.entra;

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
public class EntraUserGroupMembershipProcessor {

    private final UserEntraMembershipRepository repository;
    private final UserEntraMembershipPublisher publisher;
    private final EntraMembershipBatchLookup batchLookup;

    public void processBatch(List<ConsumerRecord<String, EntraUserGroupMembershipResult>> records) {
        Set<EntraMembershipBatchLookup.MembershipKey> requested = new LinkedHashSet<>();
        for (ConsumerRecord<String, EntraUserGroupMembershipResult> record : records) {
            EntraUserGroupMembershipResult response = record.value();
            if (response != null && response.getCode() != null
                    && response.getEntraUserRef() != null && response.getEntraGroupRef() != null) {
                requested.add(new EntraMembershipBatchLookup.MembershipKey(
                        response.getEntraUserRef(), response.getEntraGroupRef()));
            }
        }

        if (requested.isEmpty()) {
            for (ConsumerRecord<String, EntraUserGroupMembershipResult> record : records) {
                logInvalid(record);
            }
            log.info("Processed Entra user membership batch: received={}, matched=0", records.size());
            return;
        }

        Map<EntraMembershipBatchLookup.MembershipKey, UserEntraMembership> memberships = new HashMap<>();
        boolean useIndividualLookup = false;
        try {
            batchLookup.findAll(requested, EntraMembershipBatchLookup.MembershipTable.USER, repository)
                    .forEach(membership -> memberships.put(
                            new EntraMembershipBatchLookup.MembershipKey(
                                    membership.getUserEntraId(), membership.getResourceEntraId()), membership));
        } catch (RuntimeException exception) {
            log.error("Batch lookup failed; looking up user Entra memberships individually", exception);
            useIndividualLookup = true;
        }

        Set<UserEntraMembership> changed = new LinkedHashSet<>();
        int failedRecords = 0;
        for (ConsumerRecord<String, EntraUserGroupMembershipResult> record : records) {
            try {
                EntraUserGroupMembershipResult response = record.value();
                if (response == null || response.getCode() == null
                        || response.getEntraUserRef() == null || response.getEntraGroupRef() == null) {
                    logInvalid(record);
                    continue;
                }

                UserEntraMembership membership = useIndividualLookup
                        ? repository.findByUserEntraIdAndResourceEntraId(
                                response.getEntraUserRef(), response.getEntraGroupRef()).orElse(null)
                        : memberships.get(new EntraMembershipBatchLookup.MembershipKey(
                                response.getEntraUserRef(), response.getEntraGroupRef()));
                if (membership == null) {
                    log.warn("No user Entra membership found for user {} and resource {}, messageKey: {}",
                            response.getEntraUserRef(), response.getEntraGroupRef(), record.key());
                    continue;
                }

                log.debug("Received response for user {} in group {}, messageKey: {}",
                        response.getEntraUserRef(), response.getEntraGroupRef(), record.key());
                switch (response.getCode()) {
                    case ADDED -> confirmMembershipAdded(membership, record.key());
                    case ALREADY_PRESENT -> {
                        log.debug("User {} was already present in Entra group {} when ADD was requested, messageKey: {}",
                                response.getEntraUserRef(), response.getEntraGroupRef(), record.key());
                        confirmMembershipAdded(membership, record.key());
                    }
                    case REMOVED -> confirmMembershipRemoved(membership, record.key());
                    case ALREADY_REMOVED -> {
                        log.debug("User {} was already absent from Entra group {} when REMOVE was requested, messageKey: {}",
                                response.getEntraUserRef(), response.getEntraGroupRef(), record.key());
                        confirmMembershipRemoved(membership, record.key());
                    }
                    case ERROR -> {
                        log.warn("Received error for user {} in group {}, messageKey: {}",
                                membership.getUserEntraId(), membership.getResourceEntraId(), record.key());
                        membership.setEntraStatus(EntraMembershipSyncStatus.ERROR);
                    }
                    case FAILED -> {
                        log.warn("Received failed status for user {} in group {}, messageKey: {}",
                                membership.getUserEntraId(), membership.getResourceEntraId(), record.key());
                        membership.setEntraStatus(EntraMembershipSyncStatus.NEEDS_REPUBLISH);
                    }
                }
                changed.add(membership);
            } catch (RuntimeException exception) {
                failedRecords++;
                log.error("Failed to process user Entra membership result. topic={}, partition={}, offset={}, key={}",
                        record.topic(), record.partition(), record.offset(), record.key(), exception);
            }
        }
        int failedSaves = 0;
        if (!changed.isEmpty()) {
            try {
                repository.saveAll(changed);
            } catch (RuntimeException exception) {
                log.error("Batch save failed; saving {} user Entra memberships individually", changed.size(), exception);
                for (UserEntraMembership membership : changed) {
                    try {
                        repository.save(membership);
                    } catch (RuntimeException recordFailure) {
                        failedSaves++;
                        log.error("Failed to save user Entra membership. userId={}, groupId={}",
                                membership.getUserEntraId(), membership.getResourceEntraId(), recordFailure);
                    }
                }
            }
        }
        if (failedRecords > 0 || failedSaves > 0) {
            log.error("Entra user membership batch completed with failures: received={}, failedRecords={}, failedSaves={}. Failed results were logged and skipped.",
                    records.size(), failedRecords, failedSaves);
        } else {
            log.info("Processed Entra user membership batch of {} records.",
                    records.size());
        }
    }

    private void logInvalid(ConsumerRecord<String, EntraUserGroupMembershipResult> record) {
        if (record.value() == null) {
            log.warn("Skipping null Entra user membership result for key {}", record.key());
        } else {
            log.warn("Skipping Entra user membership result with missing code or user/group ID for key {}", record.key());
        }
    }

    private void confirmMembershipRemoved(UserEntraMembership membership, String key) {
        if (membership.getMembershipStatus() != MembershipStatus.INACTIVE) {
            log.debug("Received confirmation for removal of user {} from group {}, messageKey: {}",
                    membership.getUserEntraId(), membership.getResourceEntraId(), key);
            publisher.publish(membership, true);
        } else {
            membership.setEntraStatus(EntraMembershipSyncStatus.DELETION_CONFIRMED);
        }
    }

    private void confirmMembershipAdded(UserEntraMembership membership, String key) {
        if (membership.getMembershipStatus() != MembershipStatus.ACTIVE) {
            log.debug("Received confirmation for addition of user {} to group {}, messageKey: {}",
                    membership.getUserEntraId(), membership.getResourceEntraId(), key);
            publisher.publish(membership, true);
        } else {
            membership.setEntraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);
        }
    }

}
