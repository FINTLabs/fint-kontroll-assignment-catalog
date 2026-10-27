package no.fintlabs.assignment.device.projection;

import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import jakarta.persistence.EntityManager;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.*;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import no.fintlabs.enforcement.LicenseEnforcementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

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
        verifyNoInteractions(deviceEntraMembershipRepository);
    }

    @Test
    void createFlattenedAssignments_shouldCreateAssignments_whenActiveMembershipsExist() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setEntraGroupId(UUID.randomUUID());
        assignment.setRoleRef(50L);

        Device device = new Device();
        device.setId(10L);
        device.setDataObjectId(UUID.randomUUID());

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .device(device)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(List.of(membership));

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(any(), any()))
                .thenReturn(Optional.empty());

        when(deviceRepository.findById(any())).thenReturn(Optional.of(device));

        Set<FlattenedDeviceAssignment> result = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);

        assertEquals(1, result.size());
        FlattenedDeviceAssignment fda = result.iterator().next();
        assertEquals(device.getId(), fda.getDeviceRef());
        assertNotNull(fda.getDeviceEntraMembership());
        assertEquals(MembershipStatus.ACTIVE, fda.getDeviceEntraMembership().getMembershipStatus());
        assertEquals(EntraMembershipSyncStatus.NOT_SENT, fda.getDeviceEntraMembership().getEntraStatus());
    }

    @Test
    void createFlattenedAssignments_shouldResetAzureInfo_whenItExistsButIsInactive() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setEntraGroupId(UUID.randomUUID());

        Device device = new Device();
        device.setId(10L);
        device.setDataObjectId(UUID.randomUUID());

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .device(device)
                .build();

        DeviceEntraMembership existingInfo = DeviceEntraMembership.builder()
                .membershipStatus(MembershipStatus.INACTIVE)
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(100L))
                .thenReturn(List.of(membership));

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(any(), any()))
                .thenReturn(Optional.of(existingInfo));

        when(deviceRepository.findById(any())).thenReturn(Optional.of(device));

        Set<FlattenedDeviceAssignment> result = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);

        assertEquals(1, result.size());
        FlattenedDeviceAssignment fda = result.iterator().next();
        assertEquals(existingInfo, fda.getDeviceEntraMembership());
        assertEquals(MembershipStatus.ACTIVE, fda.getDeviceEntraMembership().getMembershipStatus());
        assertEquals(EntraMembershipSyncStatus.NOT_SENT, fda.getDeviceEntraMembership().getEntraStatus());
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldSaveAndPublish_whenStatusIsNotSent() {
        DeviceEntraMembership info = DeviceEntraMembership.builder()
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .id(1L)
                .assignmentId(10L)
                .deviceEntraMembership(info)
                .build();

        List<FlattenedDeviceAssignment> assignments = List.of(fda);

        when(flattenedDeviceAssignmentRepository.saveAll(assignments)).thenReturn(assignments);

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments);

        verify(flattenedDeviceAssignmentRepository).saveAll(assignments);
        verify(deviceAssigmentEntityProducerService).publish(info, false);
        verify(flattenedDeviceAssignmentRepository).flush();
        verify(entityManager).clear();
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldNotPublish_whenStatusIsSent() {
        DeviceEntraMembership info = DeviceEntraMembership.builder()
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .id(1L)
                .assignmentId(10L)
                .deviceEntraMembership(info)
                .build();

        List<FlattenedDeviceAssignment> assignments = List.of(fda);

        when(flattenedDeviceAssignmentRepository.saveAll(assignments)).thenReturn(assignments);

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments);

        verify(flattenedDeviceAssignmentRepository).saveAll(assignments);
        verify(deviceAssigmentEntityProducerService, never()).publish(any(), anyBoolean());
    }

    @Test
    void saveAndPublishFlattenedAssignmentsBatch_shouldNotPublish_whenPublishingIsDisabled() {
        DeviceEntraMembership info = DeviceEntraMembership.builder()
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .id(1L)
                .assignmentId(10L)
                .deviceEntraMembership(info)
                .build();

        List<FlattenedDeviceAssignment> assignments = List.of(fda);
        when(flattenedDeviceAssignmentRepository.saveAll(assignments)).thenReturn(assignments);

        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(assignments, false);

        verify(flattenedDeviceAssignmentRepository).saveAll(assignments);
        verify(deviceAssigmentEntityProducerService, never()).publish(any(), anyBoolean());
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
                    .deviceEntraMembership(DeviceEntraMembership.builder().deviceEntraId(UUID.randomUUID()).resourceEntraId(UUID.randomUUID())
                            .entraStatus(EntraMembershipSyncStatus.NOT_SENT).membershipStatus(MembershipStatus.ACTIVE).build()).build());
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
        DeviceEntraMembership azureInfo = mock(DeviceEntraMembership.class);

        when(fda.getDeviceEntraMembership()).thenReturn(azureInfo);
        // Important: this condition triggers the publish logic
        when(azureInfo.getEntraStatus()).thenReturn(EntraMembershipSyncStatus.SENT);

        when(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "reason");

        verify(fda).setTerminationReason("reason");
        verify(fda).setTerminationDate(any());
        verify(flattenedDeviceAssignmentRepository).saveAll(any());
        verify(entityManager).flush();

        verify(deviceAssigmentEntityProducerService).publish(azureInfo, true);
        verify(azureInfo).setMembershipStatus(MembershipStatus.INACTIVE);
    }

    @Test
    void deleteFlattenedDeviceAssignments_shouldNotPublishDeactivation_ifAssignmentsRemain() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentRemovedDate(new Date());

        FlattenedDeviceAssignment fda = mock(FlattenedDeviceAssignment.class);
        DeviceEntraMembership azureInfo = mock(DeviceEntraMembership.class);

        when(fda.getDeviceEntraMembership()).thenReturn(azureInfo);
        when(flattenedDeviceAssignmentRepository.existsByDeviceEntraMembershipAndTerminationDateIsNull(azureInfo))
                .thenReturn(true);

        when(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "reason");

        verify(flattenedDeviceAssignmentRepository).saveAll(any());
        verify(deviceAssigmentEntityProducerService, never()).publish(any(), anyBoolean());
    }

    @Test
    void syncFlattenedAssignments_shouldCreateNewAndTerminateOld() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setDeviceGroupRef(100L);
        assignment.setEntraGroupId(UUID.randomUUID());

        // Existing
        FlattenedDeviceAssignment existingFda = FlattenedDeviceAssignment.builder()
                .id(100L)
                .deviceRef(10L)
                .deviceEntraMembership(mock(DeviceEntraMembership.class))
                .assignmentId(assignment.getId())
                .build();
        when(existingFda.getDeviceEntraMembership().getEntraStatus()).thenReturn(EntraMembershipSyncStatus.SENT);

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

        when(deviceEntraMembershipRepository.findByDeviceEntraIdAndResourceEntraId(any(), any()))
                .thenReturn(Optional.empty());

        // Mock saving new
        when(flattenedDeviceAssignmentRepository.saveAll(argThat((List<FlattenedDeviceAssignment> list) ->
                list.stream().anyMatch(f -> f.getDeviceRef() == 20L)
        ))).thenAnswer(inv -> inv.getArgument(0));

        when(deviceRepository.findById(any())).thenReturn(Optional.of(device20));
        flattenedDeviceAssignmentService.syncFlattenedAssignments(assignment, true);

        // Verify Termination of old (device 10)
        assertNotNull(existingFda.getTerminationDate());
        assertEquals("Assignment sync – flattened assignment no longer valid", existingFda.getTerminationReason());
        verify(flattenedDeviceAssignmentRepository).saveAll(argThat((Collection<FlattenedDeviceAssignment> c) -> c.contains(existingFda)));

        // Verify Creation of new (device 20)
        verify(flattenedDeviceAssignmentRepository).saveAll(argThat((List<FlattenedDeviceAssignment> list) ->
                list.size() == 1 && list.getFirst().getDeviceRef() == 20L
        ));
    }

    @Test
    void deactivateAssignmentsForMembership_shouldTerminateAndPublish() {
        FlattenedDeviceAssignment fda = mock(FlattenedDeviceAssignment.class);
        DeviceEntraMembership azureInfo = mock(DeviceEntraMembership.class);

        when(fda.getDeviceEntraMembership()).thenReturn(azureInfo);
        when(azureInfo.getEntraStatus()).thenReturn(EntraMembershipSyncStatus.SENT);

        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndAssignmentViaGroupRefAndTerminationDateIsNull(1L, 100L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deactivateAssignmentsForMembership(1L, 100L);

        verify(fda).setTerminationDate(any());
        verify(fda).setTerminationReason("Role membership deactivated");
        verify(flattenedDeviceAssignmentRepository).saveAll(anyList());
        verify(deviceAssigmentEntityProducerService).publish(azureInfo, true);
    }

    @Test
    void deactivateAndDeleteAssignmentsForDevice_shouldTerminatePublishAndDeleteRows() {
        DeviceEntraMembership azureInfo = DeviceEntraMembership.builder()
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .assignmentId(10L)
                .deviceRef(1L)
                .deviceEntraMembership(azureInfo)
                .build();

        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.deactivateAndDeleteAssignmentsForDevice(1L);

        assertNotNull(fda.getTerminationDate());
        assertEquals("Device deleted", fda.getTerminationReason());
        assertEquals(MembershipStatus.INACTIVE, azureInfo.getMembershipStatus());
        verify(flattenedDeviceAssignmentRepository).saveAll(List.of(fda));
        verify(deviceAssigmentEntityProducerService).publish(azureInfo, true);
        verify(flattenedDeviceAssignmentRepository).deleteAllByDeviceRef(1L);
        verify(flattenedDeviceAssignmentRepository).flush();
    }

    @Test
    void updateDeviceEntraId_shouldUpdateActiveAssignmentsAndMemberships() {
        UUID oldAzureId = UUID.randomUUID();
        UUID newAzureId = UUID.randomUUID();
        Device device = Device.builder()
                .id(1L)
                .dataObjectId(newAzureId)
                .build();
        DeviceEntraMembership azureInfo = DeviceEntraMembership.builder()
                .deviceEntraId(oldAzureId)
                .resourceEntraId(UUID.randomUUID())
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .assignmentId(10L)
                .deviceRef(1L)
                .identityProviderDeviceObjectId(oldAzureId)
                .deviceEntraMembership(azureInfo)
                .build();

        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.updateDeviceEntraId(device);

        assertEquals(newAzureId, fda.getIdentityProviderDeviceObjectId());
        assertEquals(newAzureId, azureInfo.getDeviceEntraId());
        verify(flattenedDeviceAssignmentRepository).saveAll(List.of(fda));
        verify(deviceEntraMembershipRepository).saveAll(any());
        verify(deviceAssigmentEntityProducerService).publish(azureInfo, false);
    }

    @Test
    void updateDeviceEntraId_shouldForcePublishErrorMemberships() {
        UUID newAzureId = UUID.randomUUID();
        Device device = Device.builder()
                .id(1L)
                .dataObjectId(newAzureId)
                .build();
        DeviceEntraMembership azureInfo = DeviceEntraMembership.builder()
                .deviceEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .entraStatus(EntraMembershipSyncStatus.ERROR)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedDeviceAssignment fda = FlattenedDeviceAssignment.builder()
                .assignmentId(10L)
                .deviceRef(1L)
                .deviceEntraMembership(azureInfo)
                .build();

        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(1L))
                .thenReturn(List.of(fda));

        flattenedDeviceAssignmentService.updateDeviceEntraId(device);

        verify(deviceAssigmentEntityProducerService).publish(azureInfo, true);
    }

    @Test
    void updateDeviceEntraId_shouldPublishSharedMembershipOnceAfterCommit() {
        UUID newEntraId = UUID.randomUUID();
        Device device = Device.builder().id(1L).dataObjectId(newEntraId).build();
        DeviceEntraMembership membership = DeviceEntraMembership.builder()
                .deviceEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .entraStatus(EntraMembershipSyncStatus.SENT)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
        FlattenedDeviceAssignment firstAssignment = FlattenedDeviceAssignment.builder()
                .id(10L)
                .assignmentId(100L)
                .deviceRef(1L)
                .deviceEntraMembership(membership)
                .build();
        FlattenedDeviceAssignment secondAssignment = FlattenedDeviceAssignment.builder()
                .id(11L)
                .assignmentId(101L)
                .deviceRef(1L)
                .deviceEntraMembership(membership)
                .build();
        when(flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(1L))
                .thenReturn(List.of(firstAssignment, secondAssignment));

        TransactionSynchronizationManager.initSynchronization();
        try {
            flattenedDeviceAssignmentService.updateDeviceEntraId(device);

            assertEquals(newEntraId, firstAssignment.getIdentityProviderDeviceObjectId());
            assertEquals(newEntraId, secondAssignment.getIdentityProviderDeviceObjectId());
            assertEquals(newEntraId, membership.getDeviceEntraId());
            verifyNoInteractions(deviceAssigmentEntityProducerService);
            assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(deviceAssigmentEntityProducerService).publish(membership, false);
            verifyNoMoreInteractions(deviceAssigmentEntityProducerService);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void addAssignmentsForMembership_shouldSkipInactiveMembership() {
        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .deviceId(1L)
                .deviceGroupId(100L)
                .membershipStatus("INACTIVE")
                .build();

        flattenedDeviceAssignmentService.addAssignmentsForMembership(membership);

        verifyNoInteractions(assignmentRepository);
        verify(flattenedDeviceAssignmentRepository, never()).saveAll(anyList());
        verify(deviceAssigmentEntityProducerService, never()).publish(any(), anyBoolean());
    }
}
