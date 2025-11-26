package no.fintlabs.device.azure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.AzureStatus;
import no.fintlabs.device.Device;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.device.azureInfo.DeviceAzureInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceMembershipRepublisher {

    private final DeviceAzureInfoRepository deviceAzureInfoRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @Scheduled(cron = "0 15 12 * * ?")
    public void republishFailedAssignments() {
        List<DeviceAzureInfo> deviceAzureInfos = deviceAzureInfoRepository.findAllByAzureStatus(AzureStatus.NEEDS_REPUBLISH);
        log.info("Sending to azure device assignments that need republishing. Found {} entries", deviceAzureInfos.size());
        deviceAzureInfos.forEach(deviceAssigmentEntityProducerService::publish);
    }

    public void republishErrorAssignmentsForDevice(Device device) {
        List<DeviceAzureInfo> deviceAzureInfos = deviceAzureInfoRepository.findAllByAzureStatusAndDeviceAzureId(AzureStatus.ERROR, UUID.fromString(device.getSourceId()));
        log.info("Sending to azure device with id {} assignments that returned an error before. Found {} entries", device.getId(), deviceAzureInfos.size());
        deviceAzureInfos.forEach(deviceAssigmentEntityProducerService::publish);
    }


}
