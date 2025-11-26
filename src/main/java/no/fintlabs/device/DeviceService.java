package no.fintlabs.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentService;
import no.fintlabs.device.azure.DeviceMembershipRepublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMembershipRepublisher deviceMembershipRepublisher;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    public void deleteDevice(String deviceId) {
        Long id = Long.valueOf(deviceId);
        deviceRepository.deleteById(id);
        log.info("Deleted device {}", id);
    }

    private Device mapFromIncoming(Device incoming) {
        return Device.builder()
                .id(incoming.getId())
                .sourceId(incoming.getSourceId())
                .deviceType(incoming.getDeviceType())
                .platform(incoming.getPlatform())
                .serialNumber(incoming.getSerialNumber())
                .name(incoming.getName())
                .isShared(incoming.getIsShared())
                .isPrivateProperty(incoming.getIsPrivateProperty())
                .status(incoming.getStatus())
                .statusChanged(incoming.getStatusChanged())
                .dataObjectId(incoming.getDataObjectId())
                .build();
    }

    private Device mapExistingFromIncoming(Device existing, Device incoming) {
        return existing.toBuilder()
                .sourceId(incoming.getSourceId())
                .deviceType(incoming.getDeviceType())
                .platform(incoming.getPlatform())
                .serialNumber(incoming.getSerialNumber())
                .name(incoming.getName())
                .isShared(incoming.getIsShared())
                .isPrivateProperty(incoming.getIsPrivateProperty())
                .status(incoming.getStatus())
                .statusChanged(incoming.getStatusChanged())
                .dataObjectId(incoming.getDataObjectId())
                .build();
    }

    public void saveOrUpdate(Device incomingDevice){
     deviceRepository.findById(incomingDevice.getId()).ifPresentOrElse( existing ->
             updateDevice(existing, incomingDevice), () -> createNewDevice(incomingDevice));
    }

    private void updateDevice(Device existing, Device incomingDevice) {
        if(existing.equals(incomingDevice))
        {
            log.info("Device {} already exists and has no changes", existing.getId());
            return;
        }
        Device saved = deviceRepository.save(mapExistingFromIncoming(existing, incomingDevice));
        log.info("Updated device {} with sourceId {}", existing.getId(), existing.getSourceId());

        if(!incomingDevice.getDataObjectId().equals(existing.getDataObjectId()))
        {
            deviceMembershipRepublisher.republishErrorAssignmentsForDevice(saved);
        }
        else {
            flattenedDeviceAssignmentService.updateDeviceAzureId(saved);
        }
    }

    private void createNewDevice(Device incoming) {
        Device newDevice = mapFromIncoming(incoming);
        deviceRepository.save(newDevice);
        log.info("Saved new device {} with sourceId {}", newDevice.getId(), newDevice.getSourceId());
    }
}
