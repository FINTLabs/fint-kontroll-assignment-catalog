package no.fintlabs.assignment.device.entra;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.Device;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DeviceEntraMembershipRepublisher {

    private static final int BATCH_SIZE = 500;
    private static final Duration FAILED_RETRY_DELAY = Duration.ofHours(24);

    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final DeviceEntraMembershipPublisher publisher;
    private final long responseTimeoutHours;

    public DeviceEntraMembershipRepublisher(
            DeviceEntraMembershipRepository deviceEntraMembershipRepository,
            DeviceEntraMembershipPublisher publisher,
            @Value("${fint.kontroll.assignment-catalog.reconciliation.response-timeout:72}") long responseTimeoutHours
    ) {
        this.deviceEntraMembershipRepository = deviceEntraMembershipRepository;
        this.publisher = publisher;
        this.responseTimeoutHours = responseTimeoutHours;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.reconciliation.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.reconciliation.fixed-delay}"
    )
    public void republishStaleMemberships() {
        Instant now = Instant.now();
        Date responseCutoff = Date.from(now.minus(Duration.ofHours(responseTimeoutHours)));
        Date failedCutoff = Date.from(now.minus(FAILED_RETRY_DELAY));
        log.info("Republishing device Entra memberships without a response since {} or with a failure before {}", responseCutoff, failedCutoff);

        retryStaleMemberships(responseCutoff, failedCutoff);
    }

    public void retryStaleMemberships(Date responseCutoff, Date failedCutoff) {
        long lastProcessedId = 0L;
        List<DeviceEntraMembership> memberships;

        do {
            memberships = deviceEntraMembershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                    List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED),
                    MembershipStatus.ACTIVE,
                    EntraMembershipSyncStatus.SENT,
                    MembershipStatus.INACTIVE,
                    EntraMembershipSyncStatus.DELETION_SENT,
                    EntraMembershipSyncStatus.NEEDS_REPUBLISH,
                    responseCutoff,
                    failedCutoff,
                    lastProcessedId,
                    PageRequest.of(0, BATCH_SIZE));

            memberships.forEach(membership -> publisher.publish(membership, false));

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus.ERROR, device.getDataObjectId());
        log.info("Sending to kafka device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> publisher.publish(membership, true));
    }


}
