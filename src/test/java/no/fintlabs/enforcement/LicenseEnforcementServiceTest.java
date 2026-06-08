package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentRepository;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.kodeverk.Handhevingstype;
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
    void updateAssignedLicenseReturnsFalseWhenResourceLimitWouldBeExceededByActiveMembershipCount() {
        resource.setResourceLimit(4L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isFalse();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(100L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(30L);
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent, never()).updateResourceAvailability(applicationResourceLocation, resource);
    }

    @Test
    void updateAssignedLicenseReturnsFalseWhenLocationLimitWouldBeExceededByActiveMembershipCount() {
        applicationResourceLocation.setResourceLimit(2L);
        givenLockedResourceAndLocation();
        givenActiveMembershipCounts(2L, 3L, 1L, 2L);

        boolean updated = licenseEnforcementService.recalculateAssignedResources(assignment);

        assertThat(updated).isFalse();
        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(100L);
        assertThat(applicationResourceLocation.getNumberOfResourcesAssigned()).isEqualTo(30L);
        verify(resourceRepository, never()).save(resource);
        verify(applicationResourceLocationRepository, never()).save(applicationResourceLocation);
        verify(resourceAvailabilityPublishingComponent, never()).updateResourceAvailability(applicationResourceLocation, resource);
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
