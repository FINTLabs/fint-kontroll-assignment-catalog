package no.fintlabs.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    public void deleteDevice(String deviceId) {
        Long id = Long.valueOf(deviceId);
        deviceRepository.findById(id).ifPresentOrElse(device -> {
            flattenedDeviceAssignmentService.deactivateAndDeleteAssignmentsForDevice(id);
            deviceGroupMembershipRepository.deleteAllByDeviceId(id);
            deviceGroupMembershipRepository.flush();
            deviceRepository.delete(device);
            log.info("Deleted device {}", id);
        }, () -> log.info("Received tombstone for device {}, but it did not exist", id));
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
        boolean dataObjectIdChanged = !Objects.equals(incomingDevice.getDataObjectId(), existing.getDataObjectId());
        Device saved = deviceRepository.save(mapExistingFromIncoming(existing, incomingDevice));
        log.info("Updated device {} with sourceId {}", existing.getId(), existing.getSourceId());

        if(dataObjectIdChanged)
        {
            flattenedDeviceAssignmentService.updateDeviceEntraId(saved);
        }
    }

    private void createNewDevice(Device incoming) {
        Device newDevice = mapFromIncoming(incoming);
        deviceRepository.save(newDevice);
        log.info("Saved new device {} with sourceId {}", newDevice.getId(), newDevice.getSourceId());
    }
}
