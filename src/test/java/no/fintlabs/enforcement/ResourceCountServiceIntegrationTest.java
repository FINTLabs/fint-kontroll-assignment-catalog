package no.fintlabs.enforcement;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.device.Device;
import no.fintlabs.device.DeviceRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentRepository;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest(showSql = false)
@Testcontainers
@Import(ResourceCountService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResourceCountServiceIntegrationTest extends DatabaseIntegrationTest {

    private static final Long RESOURCE_ID = 10_001L;
    private static final String ORG_1 = "org1";
    private static final String ORG_2 = "org2";

    private final UUID resourceEntraId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID otherResourceEntraId = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private ResourceCountService resourceCountService;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ApplicationResourceLocationRepository applicationResourceLocationRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DeviceGroupRepository deviceGroupRepository;
    @Autowired
    private UserEntraMembershipRepository userEntraMembershipRepository;
    @Autowired
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @Autowired
    private FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;
    @MockBean
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    @BeforeEach
    void setUp() {
        flattenedDeviceAssignmentRepository.deleteAll();
        flattenedAssignmentRepository.deleteAll();
        assignmentRepository.deleteAll();
        deviceGroupRepository.deleteAll();
        deviceRepository.deleteAll();
        deviceEntraMembershipRepository.deleteAll();
        userEntraMembershipRepository.deleteAll();
        applicationResourceLocationRepository.deleteAll();
        resourceRepository.deleteAll();
    }

    @Test
    void recalculationCountsActiveUserAndDeviceMembershipRowsAndDeduplicatesFlattenedRowsPerLocation() {
        Resource resource = saveResource(resourceEntraId, 999L);
        saveLocation(1L, ORG_1, 999L);
        saveLocation(2L, ORG_2, 999L);

        UserEntraMembership directUserMembership = saveUserMembership(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveUserFlattenedAssignment(1L, 101L, null, directUserMembership, null);

        UserEntraMembership roleMembership = saveUserMembership(
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveUserFlattenedAssignment(2L, 102L, 501L, roleMembership, null);
        saveUserFlattenedAssignment(3L, 103L, 502L, roleMembership, null);

        UserEntraMembership inactiveUserMembership = saveUserMembership(
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                resourceEntraId,
                MembershipStatus.INACTIVE);
        saveUserFlattenedAssignment(4L, 104L, 503L, inactiveUserMembership, null);

        DeviceEntraMembership firstDeviceMembership = saveDeviceMembership(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveDeviceFlattenedAssignment(5L, RESOURCE_ID, ORG_1, 601L, firstDeviceMembership, null);
        saveDeviceFlattenedAssignment(6L, RESOURCE_ID, ORG_1, 602L, firstDeviceMembership, null);

        DeviceEntraMembership secondDeviceMembership = saveDeviceMembership(
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveDeviceFlattenedAssignment(7L, RESOURCE_ID, ORG_2, 603L, secondDeviceMembership, null);

        DeviceEntraMembership inactiveDeviceMembership = saveDeviceMembership(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                resourceEntraId,
                MembershipStatus.INACTIVE);
        saveDeviceFlattenedAssignment(8L, RESOURCE_ID, ORG_2, 604L, inactiveDeviceMembership, null);

        saveUserMembership(UUID.fromString("10000000-0000-0000-0000-000000000099"), otherResourceEntraId, MembershipStatus.ACTIVE);
        saveDeviceMembership(UUID.fromString("30000000-0000-0000-0000-000000000099"), otherResourceEntraId, MembershipStatus.ACTIVE);

        resourceCountService.updateNumberOfLicenses(resource);

        Resource updatedResource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        ApplicationResourceLocation org1 = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_1)
                .getFirst();
        ApplicationResourceLocation org2 = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_2)
                .getFirst();

        assertThat(updatedResource.getNumberOfResourcesAssigned()).isEqualTo(4L);
        assertThat(org1.getNumberOfResourcesAssigned()).isEqualTo(3L);
        assertThat(org2.getNumberOfResourcesAssigned()).isEqualTo(1L);
        verify(resourceAvailabilityPublishingComponent, times(1)).updateResourceAvailability(org1, updatedResource);
        verify(resourceAvailabilityPublishingComponent, times(1)).updateResourceAvailability(org2, updatedResource);
    }

    @Test
    void recalculationIgnoresTerminatedFlattenedRowsForLocationButCountsActiveMembershipRowsForResource() {
        Resource resource = saveResource(resourceEntraId, 99L);
        saveLocation(1L, ORG_1, 99L);

        UserEntraMembership activeUserMembership = saveUserMembership(
                UUID.fromString("10000000-0000-0000-0000-000000000004"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveUserFlattenedAssignment(1L, 101L, 701L, activeUserMembership, new Date());

        DeviceEntraMembership activeDeviceMembership = saveDeviceMembership(
                UUID.fromString("30000000-0000-0000-0000-000000000004"),
                resourceEntraId,
                MembershipStatus.ACTIVE);
        saveDeviceFlattenedAssignment(2L, RESOURCE_ID, ORG_1, 801L, activeDeviceMembership, new Date());

        resourceCountService.updateNumberOfLicenses(resource);

        Resource updatedResource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        ApplicationResourceLocation org1 = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_1)
                .getFirst();

        assertThat(updatedResource.getNumberOfResourcesAssigned()).isEqualTo(2L);
        assertThat(org1.getNumberOfResourcesAssigned()).isZero();
    }

    @Test
    void recalculationSetsZeroWhenResourceHasNoEntraGroupId() {
        Resource resource = saveResource(null, 42L);
        saveLocation(1L, ORG_1, 42L);
        saveUserMembership(
                UUID.fromString("10000000-0000-0000-0000-000000000005"),
                resourceEntraId,
                MembershipStatus.ACTIVE);

        resourceCountService.updateNumberOfLicenses(resource);

        Resource updatedResource = resourceRepository.findById(RESOURCE_ID).orElseThrow();
        ApplicationResourceLocation org1 = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(RESOURCE_ID, ORG_1)
                .getFirst();

        assertThat(updatedResource.getNumberOfResourcesAssigned()).isZero();
        assertThat(org1.getNumberOfResourcesAssigned()).isZero();
    }

    private Resource saveResource(UUID entraGroupId, Long assignedResources) {
        return resourceRepository.saveAndFlush(Resource.builder()
                .id(ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .resourceId("app-" + ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .resourceName("App " + ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .resourceType("allTypes")
                .identityProviderGroupObjectId(entraGroupId)
                .numberOfResourcesAssigned(assignedResources)
                .status("ACTIVE")
                .build());
    }

    private void saveLocation(Long id, String orgUnitId, Long assignedResources) {
        applicationResourceLocationRepository.saveAndFlush(ApplicationResourceLocation.builder()
                .id(id)
                .applicationResourceId(ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .resourceId("app-" + ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .orgUnitId(orgUnitId)
                .orgUnitName("Org " + orgUnitId)
                .numberOfResourcesAssigned(assignedResources)
                .build());
    }

    private UserEntraMembership saveUserMembership(UUID userEntraId, UUID resourceEntraId, MembershipStatus status) {
        return userEntraMembershipRepository.saveAndFlush(UserEntraMembership.builder()
                .userEntraId(userEntraId)
                .resourceEntraId(resourceEntraId)
                .entraStatus(status == MembershipStatus.ACTIVE ? EntraStatus.MEMBERSHIP_CONFIRMED : EntraStatus.DELETION_CONFIRMED)
                .membershipStatus(status)
                .build());
    }

    private DeviceEntraMembership saveDeviceMembership(UUID deviceEntraId, UUID resourceEntraId, MembershipStatus status) {
        return deviceEntraMembershipRepository.saveAndFlush(DeviceEntraMembership.builder()
                .deviceEntraId(deviceEntraId)
                .resourceEntraId(resourceEntraId)
                .entraStatus(status == MembershipStatus.ACTIVE ? EntraStatus.MEMBERSHIP_CONFIRMED : EntraStatus.DELETION_CONFIRMED)
                .membershipStatus(status)
                .build());
    }

    private void saveUserFlattenedAssignment(
            Long id,
            Long assignmentId,
            Long roleRef,
            UserEntraMembership membership,
            Date terminationDate
    ) {
        flattenedAssignmentRepository.saveAndFlush(FlattenedAssignment.builder()
                .id(id)
                .assignmentId(assignmentId)
                .userRef(id + 1_000L)
                .resourceRef(ResourceCountServiceIntegrationTest.RESOURCE_ID)
                .applicationResourceLocationOrgUnitId(ResourceCountServiceIntegrationTest.ORG_1)
                .identityProviderUserObjectId(membership.getUserEntraId())
                .identityProviderGroupObjectId(membership.getResourceEntraId())
                .assignmentViaRoleRef(roleRef)
                .assignmentCreationDate(new Date())
                .assignmentTerminationDate(terminationDate)
                .userEntraMembership(membership)
                .build());
    }

    private void saveDeviceFlattenedAssignment(
            Long id,
            Long resourceRef,
            String orgUnitId,
            Long deviceGroupRef,
            DeviceEntraMembership membership,
            Date terminationDate
    ) {
        Long deviceRef = id + 2_000L;
        Long persistedAssignmentId = saveBackingDeviceAssignmentRows(resourceRef, deviceRef, deviceGroupRef);

        flattenedDeviceAssignmentRepository.saveAndFlush(FlattenedDeviceAssignment.builder()
                .id(id)
                .assignmentId(persistedAssignmentId)
                .resourceRef(resourceRef)
                .applicationResourceLocationOrgUnitId(orgUnitId)
                .identityProviderGroupObjectId(membership.getResourceEntraId())
                .assignmentCreationDate(new Date())
                .terminationDate(terminationDate)
                .deviceRef(deviceRef)
                .identityProviderDeviceObjectId(membership.getDeviceEntraId())
                .assignmentViaGroupRef(deviceGroupRef)
                .deviceEntraMembership(membership)
                .build());
    }

    private Long saveBackingDeviceAssignmentRows(Long resourceRef, Long deviceRef, Long deviceGroupRef) {
        deviceRepository.saveAndFlush(Device.builder()
                .id(deviceRef)
                .sourceId("device-source-" + deviceRef)
                .serialNumber("serial-" + deviceRef)
                .dataObjectId(UUID.randomUUID())
                .name("Device " + deviceRef)
                .deviceType("laptop")
                .platform("macos")
                .status("ACTIVE")
                .build());

        if (deviceGroupRef != null && !deviceGroupRepository.existsById(deviceGroupRef)) {
            deviceGroupRepository.saveAndFlush(DeviceGroup.builder()
                    .id(deviceGroupRef)
                    .sourceId(deviceGroupRef)
                    .name("Device group " + deviceGroupRef)
                    .deviceType("laptop")
                    .platform("macos")
                    .noOfMembers(1L)
                    .build());
        }

        Assignment assignment = assignmentRepository.saveAndFlush(Assignment.builder()
                .resourceRef(resourceRef)
                .deviceGroupRef(deviceGroupRef)
                .entraIdGroupId(resourceEntraId)
                .applicationResourceLocationOrgUnitId(ORG_1)
                .build());
        return assignment.getId();
    }
}
