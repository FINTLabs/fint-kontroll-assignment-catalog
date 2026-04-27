package no.fintlabs.device.azure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.Device;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.entraInfo.DeviceEntraInfo;
import no.fintlabs.device.entraInfo.DeviceEntraInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMembershipRepublisher {

    private final DeviceEntraInfoRepository deviceEntraInfoRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @Scheduled(cron = "0 15 12 * * ?")
    public void republishFailedAssignments() {
        List<DeviceEntraInfo> deviceEntraInfos = deviceEntraInfoRepository.findAllByEntraStatus(EntraStatus.NEEDS_REPUBLISH);
        log.info("Sending to azure device assignments that need republishing. Found {} entries", deviceEntraInfos.size());
        deviceEntraInfos.forEach(deviceAssigmentEntityProducerService::publish);
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceEntraInfo> deviceEntraInfos = deviceEntraInfoRepository.findAllByEntraStatusAndDeviceAzureId(EntraStatus.ERROR, UUID.fromString(device.getSourceId()));
        log.info("Sending to azure device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceEntraInfos.size());
        deviceEntraInfos.forEach(deviceAssigmentEntityProducerService::publish);
    }


}
