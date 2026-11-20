package no.fintlabs.device;

import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    @Mock
    private DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void saveOrUpdate_shouldCreateNewDevice_whenItDoesNotExist() {
        Device incoming = Device.builder()
                .id(1L)
                .sourceId("101L")
                .name("New Device")
                .deviceType("Laptop")
                .platform("Windows")
                .serialNumber("SN123")
                .status("Active")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.empty());

        deviceService.saveOrUpdate(incoming);

        verify(deviceRepository).save(argThat(device ->
                device.getId().equals(1L) &&
                        device.getName().equals("New Device") &&
                        device.getSerialNumber().equals("SN123") &&
                        device.getStatus().equals("Active")
        ));
    }

    @Test
    void saveOrUpdate_shouldUpdateDevice_whenItExists() {
        UUID dataObjectId = UUID.randomUUID();
        Device existing = Device.builder()
                .id(1L)
                .sourceId("101L")
                .name("Old Name")
                .status("Inactive")
                .dataObjectId(dataObjectId)
                .build();

        Device incoming = Device.builder()
                .id(1L)
                .sourceId("202L")
                .name("Updated Name")
                .status("Active")
                .statusChanged(new Date())
                .dataObjectId(dataObjectId)
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(existing));

        deviceService.saveOrUpdate(incoming);

        verify(deviceRepository).save(argThat(device ->
                device.getId().equals(1L) &&
                        "202L".equals(device.getSourceId()) &&
                        "Updated Name".equals(device.getName()) &&
                        "Active".equals(device.getStatus()) &&
                        incoming.getStatusChanged().equals(device.getStatusChanged())
        ));
    }

    @Test
    void deleteDevice_shouldDeactivateAssignmentsDeleteMembershipsAndDeleteDevice() {
        String deviceId = "123";
        Device device = Device.builder().id(123L).build();

        when(deviceRepository.findById(123L)).thenReturn(Optional.of(device));

        deviceService.deleteDevice(deviceId);

        verify(flattenedDeviceAssignmentService).deactivateAndDeleteAssignmentsForDevice(123L);
        verify(deviceGroupMembershipRepository).deleteAllByDeviceId(123L);
        verify(deviceGroupMembershipRepository).flush();
        verify(deviceRepository).delete(device);
    }

    @Test
    void saveOrUpdate_shouldUpdateActiveAssignments_whenDataObjectIdHasChanged() {
        UUID oldAzureId = UUID.randomUUID();
        UUID newAzureId = UUID.randomUUID();

        Device existing = Device.builder()
                .id(1L)
                .dataObjectId(oldAzureId)
                .name("Device")
                .build();

        Device incoming = Device.builder()
                .id(1L)
                .dataObjectId(newAzureId)
                .name("Device") // Name is same, but equals check in service will fail due to dataObjectId change
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        deviceService.saveOrUpdate(incoming);

        verify(deviceRepository).save(any());
        verify(flattenedDeviceAssignmentService).updateDeviceEntraId(argThat(device ->
                device.getId().equals(1L) && device.getDataObjectId().equals(newAzureId)
        ));
    }

    @Test
    void saveOrUpdate_shouldUpdateActiveAssignments_whenJpaMergeMutatesExistingDevice() {
        UUID oldAzureId = UUID.randomUUID();
        UUID newAzureId = UUID.randomUUID();

        Device existing = Device.builder()
                .id(1L)
                .dataObjectId(oldAzureId)
                .name("Device")
                .build();

        Device incoming = Device.builder()
                .id(1L)
                .dataObjectId(newAzureId)
                .name("Device")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any())).thenAnswer(invocation -> {
            Device saved = invocation.getArgument(0);
            existing.setDataObjectId(saved.getDataObjectId());
            return existing;
        });

        deviceService.saveOrUpdate(incoming);

        verify(flattenedDeviceAssignmentService).updateDeviceEntraId(argThat(device ->
                device.getId().equals(1L) && device.getDataObjectId().equals(newAzureId)
        ));
    }

    @Test
    void saveOrUpdate_shouldReturnEarly_whenNoChangesDetected() {
        Device existing = Device.builder()
                .id(1L)
                .name("Same")
                .status("Active")
                .build();

        // incoming is identical to existing
        Device incoming = Device.builder()
                .id(1L)
                .name("Same")
                .status("Active")
                .build();

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(existing));

        deviceService.saveOrUpdate(incoming);

        verify(deviceRepository, never()).save(any());
        verifyNoInteractions(flattenedDeviceAssignmentService);
    }

}
