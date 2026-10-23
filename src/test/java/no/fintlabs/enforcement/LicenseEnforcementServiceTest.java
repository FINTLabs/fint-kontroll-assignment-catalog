package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentRepository;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentRepository;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.membership.MembershipRepository;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LicenseEnforcementServiceTest {

    private static final UUID RESOURCE_ENTRA_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ApplicationResourceLocationRepository applicationResourceLocationRepository;
    @Mock
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;
    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    @Mock
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @Mock
    private FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private DeviceGroupMembershipRepository deviceGroupMembershipRepository;

    @InjectMocks
    private LicenseEnforcementService licenseEnforcementService;

    private Resource resource;
    private ApplicationResourceLocation applicationResourceLocation;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        resource = Resource.builder()
                .id(1L)
                .resourceId("app1")
                .resourceType("allTypes")
                .identityProviderGroupObjectId(RESOURCE_ENTRA_ID)
                .numberOfResourcesAssigned(100L)
                .resourceLimit(10L)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        applicationResourceLocation = ApplicationResourceLocation.builder()
                .id(1L)
                .applicationResourceId(1L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(10L)
                .numberOfResourcesAssigned(30L)
                .build();

        assignment = Assignment.builder()
                .id(111L)
                .userRef(333L)
                .resourceRef(1L)
                .organizationUnitId("org1")
                .applicationResourceLocationOrgUnitId("org1")
                .entraUserId(UUID.randomUUID())
                .entraGroupId(RESOURCE_ENTRA_ID)
                .build();

        ReflectionTestUtils.setField(licenseEnforcementService, "hardstopEnabled", true);
    }

    @Test
    void updateAssignedLicenseRecalculatesFromActiveMembershipRows() {
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isTrue();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(5L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(3L);
        verify(resourceRepository).save(resource);
        verify(applicationResourceLocationRepository).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(applicationResourceLocation, resource);
    }

    @Test
    void updateAssignedLicensePersistsObservedCountsWhenResourceLimitIsExceeded() {
        resource.setResourceLimit(4L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isTrue();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(5L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(3L);
        verify(resourceRepository).save(resource);
        verify(applicationResourceLocationRepository).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(applicationResourceLocation, resource);
    }

    @Test
    void updateAssignedLicensePersistsObservedCountsWhenLocationLimitIsExceeded() {
        applicationResourceLocation.setResourceLimit(2L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isTrue();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(5L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(3L);
        verify(resourceRepository).save(resource);
        verify(applicationResourceLocationRepository).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(applicationResourceLocation, resource);
    }

    @Test
    void updateAssignedLicenseUpdatesResourceWhenNoLocationsExist() {
        given(resourceRepository.lockByResourceId(1L)).willReturn(resource);
        given(applicationResourceLocationRepository.lockByApplicationResourceId(1L)).willReturn(List.of());
        given(userEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE))
                .willReturn(2L);
        given(deviceEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE))
                .willReturn(3L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isTrue();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(5L);
        verify(resourceRepository).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent, never()).updateResourceAvailability(applicationResourceLocation, resource);
    }

    @Test
    void validateAssignmentCapacityAllowsProjectedDirectUserMembershipAtLimit() {
        resource.setResourceLimit(10L);
        applicationResourceLocation.setResourceLimit(10L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(9L, 0L, 9L, 0L);
        given(userEntraMembershipRepository.existsByUserEntraIdAndResourceEntraIdAndMembershipStatus(
                assignment.getEntraUserId(),
                RESOURCE_ENTRA_ID,
                MembershipStatus.ACTIVE))
                .willReturn(false);

        boolean allowed = licenseEnforcementService.validateAssignmentCapacity(assignment);

        assertThat(allowed).isTrue();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(100L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(30L);
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
    }

    @Test
    void validateAssignmentCapacityReturnsFalseWhenProjectedDirectUserMembershipExceedsLimit() {
        resource.setResourceLimit(9L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(9L, 0L, 1L, 0L);
        given(userEntraMembershipRepository.existsByUserEntraIdAndResourceEntraIdAndMembershipStatus(
                assignment.getEntraUserId(),
                RESOURCE_ENTRA_ID,
                MembershipStatus.ACTIVE))
                .willReturn(false);

        boolean allowed = licenseEnforcementService.validateAssignmentCapacity(assignment);

        assertThat(allowed).isFalse();
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
    }

    @Test
    void validateAssignmentCapacityAllowsDeviceGroupAtLimitWhenAllMembersAlreadyHaveLicenses() {
        resource.setResourceLimit(5L);
        applicationResourceLocation.setResourceLimit(3L);
        Assignment deviceAssignment = Assignment.builder()
                .resourceRef(1L)
                .deviceGroupRef(30L)
                .applicationResourceLocationOrgUnitId("org1")
                .build();
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);
        given(deviceGroupMembershipRepository.countActiveDeviceGroupMembersWithoutActiveEntraMembership(
                30L, RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE)).willReturn(0L);

        boolean allowed = licenseEnforcementService.validateAssignmentCapacity(deviceAssignment);

        assertThat(allowed).isTrue();
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
    }

    @Test
    void validateAssignmentCapacityRejectsDeviceGroupWhenNewMembershipWouldExceedLimit() {
        resource.setResourceLimit(5L);
        Assignment deviceAssignment = Assignment.builder()
                .resourceRef(1L)
                .deviceGroupRef(30L)
                .applicationResourceLocationOrgUnitId("org1")
                .build();
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);
        given(deviceGroupMembershipRepository.countActiveDeviceGroupMembersWithoutActiveEntraMembership(
                30L, RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE)).willReturn(1L);

        boolean allowed = licenseEnforcementService.validateAssignmentCapacity(deviceAssignment);

        assertThat(allowed).isFalse();
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
    }

    private void givenLockedResourceAndLocation() {
        given(resourceRepository.lockByResourceId(1L)).willReturn(resource);
        given(applicationResourceLocationRepository.lockByApplicationResourceId(1L))
                .willReturn(List.of(applicationResourceLocation));
    }

    private void givenActiveMembershipCounts(long userResourceCount, long deviceResourceCount, long userLocationCount, long deviceLocationCount) {
        given(userEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE))
                .willReturn(userResourceCount);
        given(deviceEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(RESOURCE_ENTRA_ID, MembershipStatus.ACTIVE))
                .willReturn(deviceResourceCount);
        given(flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org1", MembershipStatus.ACTIVE))
                .willReturn(userLocationCount);
        given(flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org1", MembershipStatus.ACTIVE))
                .willReturn(deviceLocationCount);
    }
}
