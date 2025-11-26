package no.fintlabs.device.assignment;

import no.fintlabs.DatabaseIntegrationTest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests in the same "shape" as AssignmentServiceIntegrationTest:
 * - @DataJpaTest + @Import(service under test)
 * - Real repositories for persistence assertions
 * - @MockBean for external collaborators
 */
@DataJpaTest
@Testcontainers
@Import({FlattenedDeviceAssignmentService.class})
class FlattenedDeviceAssignmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    @Autowired
    private FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;

    @Autowired
    private DeviceAzureInfoRepository deviceAzureInfoRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @MockBean
    private DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    @MockBean
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    @Test
    @Transactional
    void shouldCreateAndPersistFlattenedAssignments_andPublishWhenAzureStatusNotSent() {
        Long assignmentId = 1L;
        Long deviceGroupRef = 100L;

        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setDeviceGroupRef(deviceGroupRef);
        assignment.setAzureAdGroupId(resourceAzureId);

        Device device = new Device();
        device.setId(10L);
        device.setDataObjectId(deviceAzureId);

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .deviceId(device.getId())
                .deviceGroupId(deviceGroupRef)
                .membershipStatus("ACTIVE")
                .membershipStatusChanged(new Date())
                .device(device)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(deviceGroupRef))
                .thenReturn(List.of(membership));

        // Act (avoid @Async wrapper for determinism)
        var flattened = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattened.stream().toList());

        // Assert flattened rows
        List<FlattenedDeviceAssignment> allFlattened = flattenedDeviceAssignmentRepository.findAll();
        assertThat(allFlattened).hasSize(1);

        FlattenedDeviceAssignment fda = allFlattened.getFirst();
        assertThat(fda.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(fda.getDeviceRef()).isEqualTo(device.getId());
        assertThat(fda.getIdentityProviderDeviceObjectId()).isEqualTo(deviceAzureId);

        // Assert azure info persisted/linked
        DeviceAzureInfo azureInfo = fda.getAzureInfo();
        assertThat(azureInfo).isNotNull();
        assertThat(azureInfo.getId()).isNotNull();
        assertThat(azureInfo.getDeviceAzureId()).isEqualTo(deviceAzureId);
        assertThat(azureInfo.getResourceAzureId()).isEqualTo(resourceAzureId);
        assertThat(azureInfo.getAzureStatus()).isEqualTo(AzureStatus.NOT_SENT);
        assertThat(azureInfo.getKontrollStatus()).isEqualTo(KontrollStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceAzureInfo.class));
    }

    @Test
    @Transactional
    void shouldReuseExistingAzureInfo_whenExistsAndIsActive() {
        Long assignmentId = 2L;
        Long deviceGroupRef = 200L;

        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        DeviceAzureInfo existing = deviceAzureInfoRepository.saveAndFlush(
                DeviceAzureInfo.builder()
                        .deviceAzureId(deviceAzureId)
                        .resourceAzureId(resourceAzureId)
                        .azureStatus(AzureStatus.SENT)
                        .kontrollStatus(KontrollStatus.ACTIVE)
                        .build()
        );

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setDeviceGroupRef(deviceGroupRef);
        assignment.setAzureAdGroupId(resourceAzureId);

        Device device = new Device();
        device.setId(20L);
        device.setDataObjectId(deviceAzureId);

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .deviceId(device.getId())
                .deviceGroupId(deviceGroupRef)
                .membershipStatus("ACTIVE")
                .membershipStatusChanged(new Date())
                .device(device)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(deviceGroupRef))
                .thenReturn(List.of(membership));

        var flattened = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattened.stream().toList());

        List<FlattenedDeviceAssignment> allFlattened = flattenedDeviceAssignmentRepository.findAll();
        assertThat(allFlattened).hasSize(1);

        DeviceAzureInfo used = allFlattened.getFirst().getAzureInfo();
        assertThat(used.getId()).isEqualTo(existing.getId());
        assertThat(used.getAzureStatus()).isEqualTo(AzureStatus.SENT);
        assertThat(used.getKontrollStatus()).isEqualTo(KontrollStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, never()).publish(any(DeviceAzureInfo.class));
    }

    @Test
    @Transactional
    void shouldResetExistingAzureInfo_whenExistingIsInactive() {
        Long assignmentId = 3L;
        Long deviceGroupRef = 300L;

        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        DeviceAzureInfo existing = deviceAzureInfoRepository.saveAndFlush(
                DeviceAzureInfo.builder()
                        .deviceAzureId(deviceAzureId)
                        .resourceAzureId(resourceAzureId)
                        .azureStatus(AzureStatus.DELETION_SENT)
                        .kontrollStatus(KontrollStatus.INACTIVE)
                        .sentToAzureAt(new Date())
                        .deletionSentToAzureAt(new Date())
                        .build()
        );

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setDeviceGroupRef(deviceGroupRef);
        assignment.setAzureAdGroupId(resourceAzureId);

        Device device = new Device();
        device.setId(30L);
        device.setDataObjectId(deviceAzureId);

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .deviceId(device.getId())
                .deviceGroupId(deviceGroupRef)
                .membershipStatus("ACTIVE")
                .membershipStatusChanged(new Date())
                .device(device)
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(deviceGroupRef))
                .thenReturn(List.of(membership));

        var flattened = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattened.stream().toList());

        DeviceAzureInfo refreshed = deviceAzureInfoRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getAzureStatus()).isEqualTo(AzureStatus.NOT_SENT);
        assertThat(refreshed.getKontrollStatus()).isEqualTo(KontrollStatus.ACTIVE);
        assertThat(refreshed.getSentToAzureAt()).isNull();
        assertThat(refreshed.getDeletionSentToAzureAt()).isNull();

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceAzureInfo.class));
    }

    @Test
    @Transactional
    void shouldTerminateFlattenedAssignments_whenDeleteFlattenedDeviceAssignmentsCalled() {
        Long assignmentId = 4L;

        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        DeviceAzureInfo info = deviceAzureInfoRepository.saveAndFlush(
                DeviceAzureInfo.builder()
                        .deviceAzureId(deviceAzureId)
                        .resourceAzureId(resourceAzureId)
                        .azureStatus(AzureStatus.SENT)
                        .kontrollStatus(KontrollStatus.ACTIVE)
                        .build()
        );

        flattenedDeviceAssignmentRepository.saveAndFlush(
                FlattenedDeviceAssignment.builder()
                        .assignmentId(assignmentId)
                        .deviceRef(40L)
                        .identityProviderDeviceObjectId(deviceAzureId)
                        .identityProviderGroupObjectId(resourceAzureId)
                        .azureInfo(info)
                        .assignmentCreationDate(new Date())
                        .terminationDate(null)
                        .build()
        );

        Assignment assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setAssignmentRemovedDate(new Date());

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "test-reason");

        List<FlattenedDeviceAssignment> after = flattenedDeviceAssignmentRepository.findAll();
        assertThat(after).hasSize(1);
        assertThat(after.getFirst().getTerminationDate()).isNotNull();
        assertThat(after.getFirst().getTerminationReason()).isEqualTo("test-reason");
    }

    @Test
    void cornercase_shouldThrowNullPointerException_whenMembershipHasNullDevice() {
        Long deviceGroupRef = 999L;

        Assignment assignment = new Assignment();
        assignment.setId(99L);
        assignment.setDeviceGroupRef(deviceGroupRef);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        DeviceGroupMembership membership = DeviceGroupMembership.builder()
                .deviceId(1L)
                .deviceGroupId(deviceGroupRef)
                .membershipStatus("ACTIVE")
                .membershipStatusChanged(new Date())
                .device(null) // will NPE inside service
                .build();

        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(deviceGroupRef))
                .thenReturn(List.of(membership));

        assertThrows(NullPointerException.class, () -> flattenedDeviceAssignmentService.createFlattenedAssignments(assignment));
    }

    @Test
    @Transactional
    void shouldOnlyDeactivateDeviceAzureInfo_whenAllFlattenedAssignmentsAreTerminated_forSameDeviceAndResource() {
        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        DeviceAzureInfo info = deviceAzureInfoRepository.saveAndFlush(
                DeviceAzureInfo.builder()
                        .deviceAzureId(deviceAzureId)
                        .resourceAzureId(resourceAzureId)
                        .azureStatus(AzureStatus.SENT) // must be "active" for deletion publish branch
                        .kontrollStatus(KontrollStatus.ACTIVE)
                        .build()
        );

        // Two active flattened assignments pointing to the same DeviceAzureInfo (same device+resource)
        Long assignmentId1 = 101L;
        Long assignmentId2 = 102L;

        flattenedDeviceAssignmentRepository.saveAndFlush(
                FlattenedDeviceAssignment.builder()
                        .assignmentId(assignmentId1)
                        .deviceRef(1L)
                        .identityProviderDeviceObjectId(deviceAzureId)
                        .identityProviderGroupObjectId(resourceAzureId)
                        .azureInfo(info)
                        .assignmentCreationDate(new Date())
                        .terminationDate(null)
                        .build()
        );

        flattenedDeviceAssignmentRepository.saveAndFlush(
                FlattenedDeviceAssignment.builder()
                        .assignmentId(assignmentId2)
                        .deviceRef(1L)
                        .identityProviderDeviceObjectId(deviceAzureId)
                        .identityProviderGroupObjectId(resourceAzureId)
                        .azureInfo(info)
                        .assignmentCreationDate(new Date())
                        .terminationDate(null)
                        .build()
        );

        // Sanity check: two active assignments exist
        assertThat(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(assignmentId1)).hasSize(1);
        assertThat(flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(assignmentId2)).hasSize(1);

        // Terminate only the first assignment
        Assignment a1 = new Assignment();
        a1.setId(assignmentId1);
        a1.setAssignmentRemovedDate(new Date());

        testEntityManager.clear();

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(a1, "first-termination");

        // DeviceAzureInfo should NOT be marked inactive yet (one active assignment remains)
        DeviceAzureInfo afterFirst = deviceAzureInfoRepository.findById(info.getId()).orElseThrow();
        assertThat(afterFirst.getKontrollStatus()).isEqualTo(KontrollStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, never()).publish(any(DeviceAzureInfo.class));

        // Reset mock interactions to only assert behavior for the second termination
        clearInvocations(deviceAssigmentEntityProducerService);

        // Terminate the second assignment
        Assignment a2 = new Assignment();
        a2.setId(assignmentId2);
        a2.setAssignmentRemovedDate(new Date());

        testEntityManager.clear();

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(a2, "second-termination");


        // Now there are no active assignments => DeviceAzureInfo becomes INACTIVE and publish happens
        DeviceAzureInfo afterSecond = deviceAzureInfoRepository.findById(info.getId()).orElseThrow();
        assertThat(afterSecond.getKontrollStatus()).isEqualTo(KontrollStatus.INACTIVE);

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceAzureInfo.class));
    }

}