package no.fintlabs.device.assignment;

import jakarta.persistence.EntityManager;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.device.AzureStatus;
import no.fintlabs.device.Device;
import no.fintlabs.device.DeviceAssigmentEntityProducerService;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.device.azureInfo.DeviceAzureInfoRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlattenedDeviceAssignmentServiceTest {

    @Mock
    private FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;

    @Mock
    private DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    @Mock
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private DeviceAzureInfoRepository deviceAzureInfoRepository;

    @InjectMocks
    private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    @Test
    void createFlattenedAssignments_shouldReturnEmpty_whenNoActiveMemberships() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(Collections.emptyList());

        Set<FlattenedDeviceAssignment> result = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);

        assertTrue(result.isEmpty());
        verifyNoInteractions(deviceAzureInfoRepository);
    }

    @Test
    void createFlattenedAssignments_shouldCreateAssignments_whenActiveMembershipsExist() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setAzureAdGroupId(UUID.randomUUID());
        assignment.setRoleRef(50L);

        Device device = new Device();
        device.setId(10L);
        device.setDataObjectId(UUID.randomUUID());

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .device(device)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(List.of(membership));

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(any(), any()))
                .thenReturn(Optional.empty());

        Set<FlattenedDeviceAssignment> result = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);

        assertEquals(1, result.size());
        FlattenedDeviceAssignment fda = result.iterator().next();
        assertEquals(device.getId(), fda.getDeviceRef());
        assertNotNull(fda.getAzureInfo());
        assertEquals(KontrollStatus.ACTIVE, fda.getAzureInfo().getKontrollStatus());
        assertEquals(AzureStatus.NOT_SENT, fda.getAzureInfo().getAzureStatus());
    }

    @Test
    void createFlattenedAssignments_shouldResetAzureInfo_whenItExistsButIsInactive() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        Device device = new Device();
        device.setId(10L);
        device.setDataObjectId(UUID.randomUUID());

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .device(device)
                .build();

        DeviceAzureInfo existingInfo = DeviceAzureInfo.builder()
                .kontrollStatus(KontrollStatus.INACTIVE)
                .azureStatus(AzureStatus.SENT)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(List.of(membership));

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(any(), any()))
                .thenReturn(Optional.of(existingInfo));

        Set<FlattenedDeviceAssignment> result = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);

        assertEquals(1, result.size());
        FlattenedDeviceAssignment fda = result.iterator().next();
        assertEquals(existingInfo, fda.getAzureInfo());
        assertEquals(KontrollStatus.ACTIVE, fda.getAzureInfo().getKontrollStatus());
        assertEquals(AzureStatus.NOT_SENT, fda.getAzureInfo().getAzureStatus());
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldSaveAndPublish_whenStatusIsNotSent() {
        DeviceAzureInfo info = DeviceAzureInfo.builder()
                .azureStatus(AzureStatus.NOT_SENT)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .id(1L)
                .assignmentId(10L)
                .azureInfo(info)
                .build();

        List<FlattenedDeviceAssignment> assignments = List.of(fda);

        when(flattenedDeviceAssignmentRepository.saveAll(assignments)).thenReturn(assignments);

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments);

        verify(flattenedDeviceAssignmentRepository).saveAll(assignments);
        verify(deviceAssigmentEntityProducerService).publish(info);
        verify(flattenedDeviceAssignmentRepository).flush();
        verify(entityManager).clear();
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldNotPublish_whenStatusIsSent() {
        DeviceAzureInfo info = DeviceAzureInfo.builder()
                .azureStatus(AzureStatus.SENT)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .id(1L)
                .assignmentId(10L)
                .azureInfo(info)
                .build();

        List<FlattenedDeviceAssignment> assignments = List.of(fda);

        when(flattenedDeviceAssignmentRepository.saveAll(assignments)).thenReturn(assignments);

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments);

        verify(flattenedDeviceAssignmentRepository).saveAll(assignments);
        verify(deviceAssigmentEntityProducerService, never()).publish(any());
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldHandleNullAndEmptyList() {
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(null);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(Collections.emptyList());

        verifyNoInteractions(flattenedDeviceAssignmentRepository);
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldBatchLargeLists() {
        List<FlattenedDeviceAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            assignments.add(FlattenedDeviceAssignment.builder().id((long) i).assignmentId(1L)
                    .azureInfo(DeviceAzureInfo.builder().deviceAzureId(UUID.randomUUID()).resourceAzureId(UUID.randomUUID())
                            .azureStatus(AzureStatus.NOT_SENT).kontrollStatus(KontrollStatus.ACTIVE).build()).build());
        }

        // Lenient stubbing because we call saveAll twice with sublists
        lenient().when(flattenedDeviceAssignmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments);

        verify(flattenedDeviceAssignmentRepository, times(2)).saveAll(anyList());
        verify(flattenedDeviceAssignmentRepository, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    @Test
    void deleteFlattenedDeviceAssignments_shouldTerminateAndPublishDeactivation() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentRemovedDate(new Date());

        // Mocking FDA and AzureInfo to control the logic flow
        FlattenedDeviceAssignment fda = mock(FlattenedDeviceAssignment.class);
        DeviceAzureInfo azureInfo = mock(DeviceAzureInfo.class);

        when(fda.getAzureInfo()).thenReturn(azureInfo);
        // Important: this condition triggers the publish logic
        when(azureInfo.getAzureStatus()).thenReturn(AzureStatus.SENT);
        when(azureInfo.getFlattenedDeviceAssignments()).thenReturn(Collections.emptyList());

        when(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "reason");

        verify(fda).setTerminationReason("reason");
        verify(fda).setTerminationDate(any());
        verify(flattenedDeviceAssignmentRepository).saveAll(any());
        verify(entityManager).flush();

        verify(deviceAssigmentEntityProducerService).publish(azureInfo);
        verify(azureInfo).setKontrollStatus(KontrollStatus.INACTIVE);
    }

    @Test
    void deleteFlattenedDeviceAssignments_shouldNotPublishDeactivation_ifAssignmentsRemain() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentRemovedDate(new Date());

        FlattenedDeviceAssignment fda = mock(FlattenedDeviceAssignment.class);
        DeviceAzureInfo azureInfo = mock(DeviceAzureInfo.class);
        FlattenedDeviceAssignment otherFda = mock(FlattenedDeviceAssignment.class);

        when(fda.getAzureInfo()).thenReturn(azureInfo);
        when(azureInfo.getAzureStatus()).thenReturn(AzureStatus.SENT);
        when(azureInfo.getFlattenedDeviceAssignments()).thenReturn(List.of(otherFda)); // Not empty

        when(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "reason");

        verify(flattenedDeviceAssignmentRepository).saveAll(any());
        verify(deviceAssigmentEntityProducerService, never()).publish(any());
    }

    @Test
    void syncFlattenedAssignments_shouldCreateNewAndTerminateOld() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        // Existing
        FlattenedDeviceAssignment existingFda = FlattenedDeviceAssignment.builder()
                .id(100L)
                .deviceRef(10L)
                .azureInfo(mock(DeviceAzureInfo.class))
                .assignmentId(assignment.getId())
                .build();
        when(existingFda.getAzureInfo().getFlattenedDeviceAssignments()).thenReturn(Collections.emptyList());
        when(existingFda.getAzureInfo().getAzureStatus()).thenReturn(AzureStatus.SENT);

        when(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(1L))
                .thenReturn(List.of(existingFda));

        // New Requirement (Device 20L mock)
        Device device20 = new Device();
        device20.setId(20L);
        device20.setDataObjectId(UUID.randomUUID());
        DeviceGroupMembership membership20 = DeviceGroupMembership.builder().device(device20).build();

        // Note: Device 10L is NOT in memberships, so it should be terminated.
        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(List.of(membership20));

        when(deviceAzureInfoRepository.findByDeviceAzureIdAndResourceAzureId(any(), any()))
                .thenReturn(Optional.empty());

        // Mock saving new
        when(flattenedDeviceAssignmentRepository.saveAll(argThat((List<FlattenedDeviceAssignment> list) ->
                list.stream().anyMatch(f -> f.getDeviceRef() == 20L)
        ))).thenAnswer(inv -> inv.getArgument(0));

        flattenedDeviceAssignmentService.syncFlattenedAssignments(assignment);

        // Verify Termination of old (device 10)
        assertNotNull(existingFda.getTerminationDate());
        assertEquals("Assignment sync – flattened assignment no longer valid", existingFda.getTerminationReason());
        verify(flattenedDeviceAssignmentRepository).saveAll(argThat((Collection<FlattenedDeviceAssignment> c) -> c.contains(existingFda)));

        // Verify Creation of new (device 20)
        verify(flattenedDeviceAssignmentRepository).saveAll(argThat((List<FlattenedDeviceAssignment> list) ->
                list.size() == 1 && list.get(0).getDeviceRef() == 20L
        ));
    }

    @Test
    void deactivateAssignmentsForMembership_shouldTerminateAndPublish() {
        FlattenedDeviceAssignment fda = mock(FlattenedDeviceAssignment.class);
        DeviceAzureInfo azureInfo = mock(DeviceAzureInfo.class);

        when(fda.getAzureInfo()).thenReturn(azureInfo);
        when(azureInfo.getAzureStatus()).thenReturn(AzureStatus.SENT);
        when(azureInfo.getFlattenedDeviceAssignments()).thenReturn(Collections.emptyList());

        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndAssignmentViaGroupRefAndTerminationDateIsNull(1L, 100L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deactivateAssignmentsForMembership(1L, 100L);

        verify(fda).setTerminationDate(any());
        verify(fda).setTerminationReason("Role membership deactivated");
        verify(flattenedDeviceAssignmentRepository).saveAll(anyList());
        verify(deviceAssigmentEntityProducerService).publish(azureInfo);
    }
}