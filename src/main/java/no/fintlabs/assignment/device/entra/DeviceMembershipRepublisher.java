package no.fintlabs.assignment.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.device.Device;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
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
@RequiredArgsConstructor
public class DeviceMembershipRepublisher {

    private static final int REPUBLISH_BATCH_SIZE = 500;

    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;

    @Value("${fint.kontroll.assignment-catalog.reconciliation.response-timeout:72}")
    private long responseTimeoutHours;

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.reconciliation.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.reconciliation.fixed-delay}",
            timeUnit = java.util.concurrent.TimeUnit.HOURS
    )
    public void republishStaleMemberships() {
        Date cutoff = Date.from(Instant.now().minus(Duration.ofHours(responseTimeoutHours)));
        long lastProcessedId = 0L;
        List<DeviceEntraMembership> memberships;

        do {
            memberships = deviceEntraMembershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                    List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED),
                    MembershipStatus.ACTIVE,
                    List.of(EntraMembershipSyncStatus.SENT, EntraMembershipSyncStatus.NEEDS_REPUBLISH),
                    MembershipStatus.INACTIVE,
                    List.of(EntraMembershipSyncStatus.DELETION_SENT, EntraMembershipSyncStatus.NEEDS_REPUBLISH),
                    cutoff,
                    lastProcessedId,
                    PageRequest.of(0, REPUBLISH_BATCH_SIZE));

            memberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, false));

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == REPUBLISH_BATCH_SIZE);
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus.ERROR, device.getDataObjectId());
        log.info("Sending to kafka device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, true));
    }


}
