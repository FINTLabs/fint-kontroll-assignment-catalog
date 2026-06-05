package no.fintlabs.device.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.device.Device;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMembershipRepublisher {

    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @Scheduled(cron = "0 15 12 * * ?")
    public void republishFailedAssignments() {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatus(EntraStatus.NEEDS_REPUBLISH);
        log.info("Sending to kafka device assignments that need republishing. Found {} entries", deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, false));
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceEntraMembership> deviceEntraMemberships = deviceEntraMembershipRepository.findAllByEntraStatusAndDeviceEntraId(EntraStatus.ERROR, UUID.fromString(device.getSourceId()));
        log.info("Sending to kafka device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceEntraMemberships.size());
        deviceEntraMemberships.forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, true));
    }


}
