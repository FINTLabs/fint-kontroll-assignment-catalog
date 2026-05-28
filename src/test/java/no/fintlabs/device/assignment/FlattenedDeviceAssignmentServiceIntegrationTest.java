package no.fintlabs.device.assignment;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.*;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Set;
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
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceGroupRepository deviceGroupRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    @MockBean
    private DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;

    Long deviceId, deviceGroupId, assignmentId;
    UUID deviceAzureId, deviceGroupAzureId;
    Device device;
    DeviceGroup deviceGroup;
    DeviceGroupMembership membership;
    Assignment assignment;

    @BeforeEach
    void setUp() {
        deviceId = 999999L;
        deviceGroupId = 12345L;
        assignmentId =  777L;
        deviceAzureId = UUID.randomUUID();
        deviceGroupAzureId = UUID.randomUUID();

        device = Device.builder()
                .id(deviceId)
                .dataObjectId(deviceAzureId)
                .build();

        deviceRepository.saveAndFlush(device);

        deviceGroup = DeviceGroup.builder()
                .id(deviceGroupId)
                .build();

        deviceGroupRepository.saveAndFlush(deviceGroup);

        membership = DeviceGroupMembership.builder()
                .deviceId(deviceId)
                .deviceGroupId(deviceGroupId)
                .membershipStatus("ACTIVE")
                .membershipStatusChanged(new Date())
                .build();
        deviceGroupMembershipRepository.saveAndFlush(membership);

        assignment = new Assignment();
        assignment.setId(assignmentId);
        assignment.setDeviceGroupRef(deviceGroupId);
        assignment.setAzureAdGroupId(deviceGroupAzureId);
        assignmentRepository.saveAndFlush(assignment);
    }

    @Test
    @Transactional
    void createFlattenedAssignments_shouldFlattenedCreateAssignment_whenMembershipDeviceAssociationIsNull() {

        Set<FlattenedDeviceAssignment> allFlattened =flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        assertThat(allFlattened).hasSize(1);

        FlattenedDeviceAssignment fda = allFlattened.iterator().next();
        assertThat(fda.getAssignmentId()).isEqualTo(assignmentId);
        assertThat(fda.getDeviceRef()).isEqualTo(device.getId());
        assertThat(fda.getIdentityProviderDeviceObjectId()).isEqualTo(deviceAzureId);
        assertThat(fda.getIdentityProviderGroupObjectId()).isEqualTo(deviceGroupAzureId);
    }

    @Test
    @Transactional
    void shouldCreateAndPersistFlattenedAssignments_andPublishWhenAzureStatusNotSent() {
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
        DeviceEntraMembership azureInfo = fda.getDeviceEntraMembership();
        assertThat(azureInfo).isNotNull();
        assertThat(azureInfo.getId()).isNotNull();
        assertThat(azureInfo.getDeviceEntraId()).isEqualTo(deviceAzureId);
        assertThat(azureInfo.getResourceEntraId()).isEqualTo(deviceGroupAzureId);
        assertThat(azureInfo.getEntraStatus()).isEqualTo(EntraStatus.NOT_SENT);
        assertThat(azureInfo.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceEntraMembership.class), anyBoolean());
    }

    @Test
    @Transactional
    void shouldReuseExistingAzureInfo_whenExistsAndIsActive() {

        DeviceEntraMembership existing = deviceEntraMembershipRepository.saveAndFlush(
                DeviceEntraMembership.builder()
                        .deviceEntraId(deviceAzureId)
                        .resourceEntraId(deviceGroupAzureId)
                        .entraStatus(EntraStatus.SENT)
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .build()
        );
        var flattened = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattened.stream().toList());

        List<FlattenedDeviceAssignment> allFlattened = flattenedDeviceAssignmentRepository.findAll();
        assertThat(allFlattened).hasSize(1);

        DeviceEntraMembership used = allFlattened.getFirst().getDeviceEntraMembership();
        assertThat(used.getId()).isEqualTo(existing.getId());
        assertThat(used.getEntraStatus()).isEqualTo(EntraStatus.SENT);
        assertThat(used.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, never()).publish(any(DeviceEntraMembership.class), anyBoolean());
    }

    @Test
    @Transactional
    void shouldResetExistingAzureInfo_whenExistingIsInactive() {

        DeviceEntraMembership existing = deviceEntraMembershipRepository.saveAndFlush(
                DeviceEntraMembership.builder()
                        .deviceEntraId(deviceAzureId)
                        .resourceEntraId(deviceGroupAzureId)
                        .entraStatus(EntraStatus.DELETION_SENT)
                        .membershipStatus(MembershipStatus.INACTIVE)
                        .sentToEntraAt(new Date())
                        .deletionSentToEntraAt(new Date())
                        .build()
        );

        deviceGroupMembershipRepository.saveAndFlush(membership);

        var flattened = flattenedDeviceAssignmentService.createFlattenedAssignments(assignment);
        flattenedDeviceAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattened.stream().toList());

        DeviceEntraMembership refreshed = deviceEntraMembershipRepository.findById(existing.getId()).orElseThrow();
        assertThat(refreshed.getEntraStatus()).isEqualTo(EntraStatus.NOT_SENT);
        assertThat(refreshed.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(refreshed.getSentToEntraAt()).isNull();
        assertThat(refreshed.getDeletionSentToEntraAt()).isNull();

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceEntraMembership.class), anyBoolean());
    }

    @Test
    @Transactional
    void shouldTerminateFlattenedAssignments_whenDeleteFlattenedDeviceAssignmentsCalled() {

        DeviceEntraMembership info = deviceEntraMembershipRepository.saveAndFlush(
                DeviceEntraMembership.builder()
                        .deviceEntraId(deviceAzureId)
                        .resourceEntraId(deviceGroupAzureId)
                        .entraStatus(EntraStatus.SENT)
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .build()
        );

        flattenedDeviceAssignmentRepository.saveAndFlush(
                FlattenedDeviceAssignment.builder()
                        .assignmentId(assignmentId)
                        .deviceRef(40L)
                        .identityProviderDeviceObjectId(deviceAzureId)
                        .identityProviderGroupObjectId(deviceGroupAzureId)
                        .deviceEntraMembership(info)
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

//    No longer relevant as the mapToFlattenedAssignment methods now fetches the azure device id directly from the device
//    @Test
//    void cornercase_shouldThrowNullPointerException_whenMembershipHasNullDevice() {
//        Long deviceGroupRef = 999L;
//
//        Assignment assignment = new Assignment();
//        assignment.setId(99L);
//        assignment.setDeviceGroupRef(deviceGroupRef);
//        assignment.setAzureAdGroupId(UUID.randomUUID());
//
//        DeviceGroupMembership membership = DeviceGroupMembership.builder()
//                .deviceId(1L)
//                .deviceGroupId(deviceGroupId)
//                .membershipStatus("ACTIVE")
//                .membershipStatusChanged(new Date())
//                .device(null) // will NPE inside service
//                .build();
//
//        when(deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(deviceGroupId))
//                .thenReturn(List.of(membership));
//
//        assertThrows(NullPointerException.class, () -> flattenedDeviceAssignmentService.createFlattenedAssignments(assignment));
//    }

    @Test
    @Transactional
    void shouldOnlyDeactivateDeviceAzureInfo_whenAllFlattenedAssignmentsAreTerminated_forSameDeviceAndResource() {
        UUID deviceAzureId = UUID.randomUUID();
        UUID resourceAzureId = UUID.randomUUID();

        DeviceEntraMembership info = deviceEntraMembershipRepository.saveAndFlush(
                DeviceEntraMembership.builder()
                        .deviceEntraId(deviceAzureId)
                        .resourceEntraId(resourceAzureId)
                        .entraStatus(EntraStatus.SENT) // must be "active" for deletion publish branch
                        .membershipStatus(MembershipStatus.ACTIVE)
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
                        .deviceEntraMembership(info)
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
                        .deviceEntraMembership(info)
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
        DeviceEntraMembership afterFirst = deviceEntraMembershipRepository.findById(info.getId()).orElseThrow();
        assertThat(afterFirst.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(deviceAssigmentEntityProducerService, never()).publish(any(DeviceEntraMembership.class), anyBoolean());

        // Reset mock interactions to only assert behavior for the second termination
        clearInvocations(deviceAssigmentEntityProducerService);

        // Terminate the second assignment
        Assignment a2 = new Assignment();
        a2.setId(assignmentId2);
        a2.setAssignmentRemovedDate(new Date());

        testEntityManager.clear();

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(a2, "second-termination");


        // Now there are no active assignments => DeviceAzureInfo becomes INACTIVE and publish happens
        DeviceEntraMembership afterSecond = deviceEntraMembershipRepository.findById(info.getId()).orElseThrow();
        assertThat(afterSecond.getMembershipStatus()).isEqualTo(MembershipStatus.INACTIVE);

        verify(deviceAssigmentEntityProducerService, times(1)).publish(any(DeviceEntraMembership.class), anyBoolean());
    }
}