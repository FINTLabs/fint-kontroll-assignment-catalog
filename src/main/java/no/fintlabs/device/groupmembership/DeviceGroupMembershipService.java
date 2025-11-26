package no.fintlabs.device.groupmembership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.DeviceRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
class DeviceGroupMembershipService {

    private final DeviceGroupMembershipRepository deviceGroupMembershipRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceRepository deviceRepository;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    private DeviceGroupMembership mapFromIncoming(DeviceGroupMembership incoming) {
        return DeviceGroupMembership.builder()
                .deviceGroupId(incoming.getDeviceGroupId())
                .deviceId(incoming.getDeviceId())
                .membershipStatus(incoming.getMembershipStatus())
                .membershipStatusChanged(incoming.getMembershipStatusChanged())
                .build();
    }

    private DeviceGroupMembership mapExistingFromIncoming(DeviceGroupMembership existing, DeviceGroupMembership incoming) {
        return existing.toBuilder()
                .membershipStatus(incoming.getMembershipStatus())
                .membershipStatusChanged(incoming.getMembershipStatusChanged())
                .build();
    }

    public void saveOrUpdate(DeviceGroupMembership incoming) {
        validateIncomingMembership(incoming);
        deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(incoming.getDeviceId(), incoming.getDeviceGroupId())).ifPresentOrElse(existing ->
                updateDeviceGroupMembership(existing, incoming), () -> createNewDeviceGroupMembership(incoming));
    }

    private void validateIncomingMembership(DeviceGroupMembership incoming) {
        if (!deviceRepository.existsById(incoming.getDeviceId())) {
            throw new IllegalArgumentException("Device not found: " + incoming.getDeviceId());
        }
        if (!deviceGroupRepository.existsById(incoming.getDeviceGroupId())) {
            throw new IllegalArgumentException("Device group not found: " + incoming.getDeviceGroupId());
        }
    }

    private void updateDeviceGroupMembership(
            DeviceGroupMembership existing,
            DeviceGroupMembership incoming
    ) {
        DeviceGroupMembership updated =
                deviceGroupMembershipRepository.save(mapExistingFromIncoming(existing, incoming));

        log.info("Updated device group membership {}", existing.getDeviceId());

        transition(existing.getMembershipStatus(), incoming.getMembershipStatus(), updated);
    }

    private void transition(String from, String to, DeviceGroupMembership updated) {
        if ("ACTIVE".equals(from) && !"ACTIVE".equals(to)) {
            flattenedDeviceAssignmentService.deactivateAssignmentsForMembership(
                    updated.getDeviceId(),
                    updated.getDeviceGroupId()
            );
        } else if (!"ACTIVE".equals(from) && "ACTIVE".equals(to)) {
            flattenedDeviceAssignmentService.addAssignmentsForMembership(updated);
        }
    }

    private void createNewDeviceGroupMembership(DeviceGroupMembership incoming) {
        DeviceGroupMembership newDeviceGroupMembership = mapFromIncoming(incoming);
        DeviceGroupMembership saved = deviceGroupMembershipRepository.save(newDeviceGroupMembership);
        log.info("Saved new device group membership {}", newDeviceGroupMembership.getDeviceId());
        flattenedDeviceAssignmentService.addAssignmentsForMembership(saved);

    }

}
