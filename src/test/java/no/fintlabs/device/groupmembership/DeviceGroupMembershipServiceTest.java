package no.fintlabs.device.groupmembership;

import no.fintlabs.device.DeviceRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.LicenseEnforcementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceGroupMembershipServiceTest {

    @Mock private DeviceGroupMembershipRepository deviceGroupMembershipRepository;
    @Mock private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceGroupRepository deviceGroupRepository;
    @Mock private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private DeviceGroupMembershipService deviceGroupMembershipService;

    private DeviceGroupMembership membership(long deviceId, long groupId, String status) {
        return DeviceGroupMembership.builder()
                .deviceId(deviceId)
                .deviceGroupId(groupId)
                .membershipStatus(status)
                .membershipStatusChanged(new Date())
                .build();
    }

    @Test
    void saveOrUpdate_shouldCreateNewMembership_whenItDoesNotExist() {
        DeviceGroupMembership incoming = membership(1L, 100L, "ACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(true);
        when(deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(1L, 100L)))
                .thenReturn(Optional.empty());

        // service uses returned saved entity for addAssignmentsForMembership
        when(deviceGroupMembershipRepository.save(any(DeviceGroupMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        deviceGroupMembershipService.saveOrUpdate(incoming);

        verify(deviceGroupMembershipRepository).save(argThat(m ->
                m.getDeviceId().equals(1L)
                        && m.getDeviceGroupId().equals(100L)
                        && m.getMembershipStatus().equals("ACTIVE")
        ));

        // create path always adds assignments
        verify(flattenedDeviceAssignmentService).addAssignmentsForMembership(any(DeviceGroupMembership.class));

        verifyNoInteractions(licenseEnforcementService);

        verify(flattenedDeviceAssignmentService, never()).deactivateAssignmentsForMembership(anyLong(), anyLong());
    }

    @Test
    void saveOrUpdate_shouldUpdateMembership_whenItExists_andStatusUnchangedActive() {
        DeviceGroupMembership incoming = membership(1L, 100L, "ACTIVE");
        DeviceGroupMembership existing = membership(1L, 100L, "ACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(true);

        when(deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(1L, 100L)))
                .thenReturn(Optional.of(existing));

        // ensure update path gets "updated" back
        when(deviceGroupMembershipRepository.save(any(DeviceGroupMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        deviceGroupMembershipService.saveOrUpdate(incoming);

        verify(deviceGroupMembershipRepository).save(any(DeviceGroupMembership.class));

        // no assignment transition actions
        verify(flattenedDeviceAssignmentService, never()).deactivateAssignmentsForMembership(anyLong(), anyLong());
        verify(flattenedDeviceAssignmentService, never()).addAssignmentsForMembership(any(DeviceGroupMembership.class));

        // counts + enforcement always on update
    }

    @Test
    void saveOrUpdate_shouldDeactivateAssignments_whenStatusChangesFromActiveToInactive() {
        DeviceGroupMembership incoming = membership(1L, 100L, "INACTIVE");
        DeviceGroupMembership existing = membership(1L, 100L, "ACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(true);

        when(deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(1L, 100L)))
                .thenReturn(Optional.of(existing));

        when(deviceGroupMembershipRepository.save(any(DeviceGroupMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));


        deviceGroupMembershipService.saveOrUpdate(incoming);

        verify(flattenedDeviceAssignmentService).deactivateAssignmentsForMembership(1L, 100L);
        verify(flattenedDeviceAssignmentService, never()).addAssignmentsForMembership(any(DeviceGroupMembership.class));

    }

    @Test
    void saveOrUpdate_shouldAddAssignments_whenStatusChangesFromInactiveToActive() {
        DeviceGroupMembership incoming = membership(1L, 100L, "ACTIVE");
        DeviceGroupMembership existing = membership(1L, 100L, "INACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(true);

        when(deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(1L, 100L)))
                .thenReturn(Optional.of(existing));

        when(deviceGroupMembershipRepository.save(any(DeviceGroupMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));


        deviceGroupMembershipService.saveOrUpdate(incoming);

        verify(flattenedDeviceAssignmentService).addAssignmentsForMembership(any(DeviceGroupMembership.class));
        verify(flattenedDeviceAssignmentService, never()).deactivateAssignmentsForMembership(anyLong(), anyLong());


    }

    @Test
    void saveOrUpdate_shouldNotChangeAssignments_whenStatusUnchangedInactive_butStillAdjustCounts() {
        DeviceGroupMembership incoming = membership(1L, 100L, "INACTIVE");
        DeviceGroupMembership existing = membership(1L, 100L, "INACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(true);

        when(deviceGroupMembershipRepository.findById(new DeviceGroupMembershipId(1L, 100L)))
                .thenReturn(Optional.of(existing));

        when(deviceGroupMembershipRepository.save(any(DeviceGroupMembership.class)))
                .thenAnswer(inv -> inv.getArgument(0));


        deviceGroupMembershipService.saveOrUpdate(incoming);

        verify(flattenedDeviceAssignmentService, never()).addAssignmentsForMembership(any());
        verify(flattenedDeviceAssignmentService, never()).deactivateAssignmentsForMembership(anyLong(), anyLong());


    }

    @Test
    void saveOrUpdate_shouldThrow_whenDeviceDoesNotExist() {
        DeviceGroupMembership incoming = membership(1L, 100L, "ACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> deviceGroupMembershipService.saveOrUpdate(incoming));

        verify(deviceGroupRepository, never()).existsById(anyLong());
        verifyNoInteractions(deviceGroupMembershipRepository);
        verifyNoInteractions(flattenedDeviceAssignmentService);
        verifyNoInteractions(licenseEnforcementService);
    }

    @Test
    void saveOrUpdate_shouldThrow_whenGroupDoesNotExist() {
        DeviceGroupMembership incoming = membership(1L, 100L, "ACTIVE");

        when(deviceRepository.existsById(1L)).thenReturn(true);
        when(deviceGroupRepository.existsById(100L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> deviceGroupMembershipService.saveOrUpdate(incoming));

        verify(deviceGroupMembershipRepository, never()).findById(any());
        verifyNoInteractions(deviceGroupMembershipRepository);
        verifyNoInteractions(flattenedDeviceAssignmentService);
        verifyNoInteractions(licenseEnforcementService);
    }
}
