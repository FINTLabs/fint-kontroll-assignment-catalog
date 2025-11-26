package no.fintlabs.device.group;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.enforcement.LicenseEnforcementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    private DeviceGroup mapFromIncoming(DeviceGroup incoming) {
        return DeviceGroup.builder()
                .id(incoming.getId())
                .sourceId(incoming.getSourceId())
                .deviceType(incoming.getDeviceType())
                .platform(incoming.getPlatform())
                .orgUnitId(incoming.getOrgUnitId())
                .name(incoming.getName())
                .build();
    }

    private DeviceGroup mapExistingFromIncoming(DeviceGroup existing, DeviceGroup incoming) {
        return existing.toBuilder()
                .sourceId(incoming.getSourceId())
                .deviceType(incoming.getDeviceType())
                .platform(incoming.getPlatform())
                .name(incoming.getName())
                .noOfMembers(incoming.getNoOfMembers())
                .build();
    }

    public void saveOrUpdate(DeviceGroup incomingDeviceGroup) {
        deviceGroupRepository.findById(incomingDeviceGroup.getId()).ifPresentOrElse(existing ->
                updateDeviceGroup(existing, incomingDeviceGroup), () -> createNewDeviceGroup(incomingDeviceGroup));
    }

    private void updateDeviceGroup(DeviceGroup existing, DeviceGroup incomingDeviceGroup) {
        if (existing.equals(incomingDeviceGroup)) {
            log.info("Device group {} already exists and has no changes", existing.getId());
            return;
        }
        DeviceGroup saved = deviceGroupRepository.save(mapExistingFromIncoming(existing, incomingDeviceGroup));

        log.info("Updated device group {} with sourceId {}", existing.getId(), existing.getSourceId());
        if (existing.getNoOfMembers() != incomingDeviceGroup.getNoOfMembers()) {
            licenseEnforcementService.updateAssignedResourcesOnDeviceGroupUpdate(saved, incomingDeviceGroup.getNoOfMembers() - existing.getNoOfMembers());
        }
    }

    private void createNewDeviceGroup(DeviceGroup incoming) {
        DeviceGroup newDeviceGroup = mapFromIncoming(incoming);
        deviceGroupRepository.save(newDeviceGroup);
        log.info("Saved new device group {} with sourceId {}", newDeviceGroup.getId(), newDeviceGroup.getSourceId());
    }

}
