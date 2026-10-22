package no.fintlabs.assignment.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.device.Device;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMembershipRepublisher {

    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;

    @Scheduled(cron = "0 15 12 * * ?")
    public void republishFailedAssignments() {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatus(EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        log.info("Sending to kafka device assignments that need republishing. Found {} entries", deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, false));
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus.ERROR, device.getDataObjectId());
        log.info("Sending to kafka device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, true));
    }


}
