package no.fintlabs.device.group;

import no.fintlabs.enforcement.LicenseEnforcementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceGroupServiceTest {

    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private DeviceGroupService deviceGroupService;

    @Test
    void saveOrUpdate_shouldCreateNewDeviceGroup_whenItDoesNotExist() {
        DeviceGroup incoming = DeviceGroup.builder()
                .id(1L)
                .sourceId(101L)
                .name("New Group")
                .orgUnitId("org1")
                .platform("Android")
                .deviceType("Mobile")
                .noOfMembers(7L)
                .build();

        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.empty());

        deviceGroupService.saveOrUpdate(incoming);

        verify(deviceGroupRepository).save(argThat(group ->
                group.getId().equals(1L) &&
                group.getSourceId().equals(101L) &&
                group.getName().equals("New Group") &&
                group.getOrgUnitId().equals("org1") &&
                group.getNoOfMembers() == 7L
        ));
    }

    @Test
    void saveOrUpdate_shouldUpdateDeviceGroup_whenItExists() {
        DeviceGroup existing = DeviceGroup.builder()
                .id(1L)
                .sourceId(101L)
                .name("Old Name")
                .orgUnitId("org1")
                .platform("iOS")
                .deviceType("Tablet")
                .build();

        DeviceGroup incoming = DeviceGroup.builder()
                .id(1L)
                .sourceId(202L)
                .name("Updated Name")
                .orgUnitId("org2")
                .platform("Android")
                .deviceType("Phone")
                .build();

        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(existing));

        deviceGroupService.saveOrUpdate(incoming);

        verify(deviceGroupRepository).save(argThat(group ->
                group.getId().equals(1L) &&
                group.getSourceId().equals(202L) &&
                group.getName().equals("Updated Name") &&
                group.getPlatform().equals("Android") &&
                group.getOrgUnitId().equals("org2")
        ));
    }

    @Test
    void saveOrUpdate_shouldUpdateAssignedResourcesWhenMemberCountChanges() {
        DeviceGroup existing = DeviceGroup.builder()
                .id(1L)
                .sourceId(101L)
                .name("Group")
                .orgUnitId("org1")
                .platform("iOS")
                .deviceType("Tablet")
                .noOfMembers(2L)
                .build();

        DeviceGroup incoming = DeviceGroup.builder()
                .id(1L)
                .sourceId(101L)
                .name("Group")
                .orgUnitId("org1")
                .platform("iOS")
                .deviceType("Tablet")
                .noOfMembers(5L)
                .build();

        when(deviceGroupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(deviceGroupRepository.save(any(DeviceGroup.class))).thenAnswer(invocation -> {
            DeviceGroup saved = invocation.getArgument(0);
            existing.setNoOfMembers(saved.getNoOfMembers());
            return existing;
        });

        deviceGroupService.saveOrUpdate(incoming);

        verify(licenseEnforcementService).updateAssignedResourcesOnDeviceGroupUpdate(any(DeviceGroup.class));
    }
}
