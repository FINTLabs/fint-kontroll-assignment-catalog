package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentRepository;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCountServiceTest {

    @Mock
    private UserEntraMembershipRepository userEntraMembershipRepository;

    @Mock
    private DeviceEntraMembershipRepository deviceEntraMembershipRepository;

    @Mock
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Mock
    private FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;

    @Mock
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    @Mock
    private ApplicationResourceLocationRepository applicationResourceLocationRepository;

    @InjectMocks
    private ResourceCountService resourceCountService;

    @Test
    void updateNumberOfLicenses_setsResourceAndLocationsToZeroWhenNoActiveAssignments() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("resource-1")
                .numberOfResourcesAssigned(null)
                .assignments(Set.of())
                .build();
        ApplicationResourceLocation location = ApplicationResourceLocation.builder()
                .id(10L)
                .applicationResourceId(1L)
                .orgUnitId("org-1")
                .numberOfResourcesAssigned(5L)
                .build();

        when(applicationResourceLocationRepository.findByApplicationResourceId(1L)).thenReturn(List.of(location));

        resourceCountService.updateNumberOfLicenses(resource);

        assertThat(resource.getNumberOfResourcesAssigned()).isZero();
        assertThat(location.getNumberOfResourcesAssigned()).isZero();
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(location, resource);
    }

    @Test
    void updateNumberOfLicenses_countsDistinctActiveMembershipsAcrossAssignmentTypes() {
        ApplicationResourceLocation locationA = ApplicationResourceLocation.builder()
                .id(10L)
                .applicationResourceId(1L)
                .orgUnitId("org-a")
                .numberOfResourcesAssigned(99L)
                .build();
        ApplicationResourceLocation locationB = ApplicationResourceLocation.builder()
                .id(11L)
                .applicationResourceId(1L)
                .orgUnitId("org-b")
                .numberOfResourcesAssigned(99L)
                .build();
        ApplicationResourceLocation unusedLocation = ApplicationResourceLocation.builder()
                .id(12L)
                .applicationResourceId(1L)
                .orgUnitId("org-c")
                .numberOfResourcesAssigned(99L)
                .build();

        Assignment userAssignment = Assignment.builder()
                .id(100L)
                .assignmentId("user-assignment")
                .userRef(1L)
                .resourceRef(1L)
                .applicationResourceLocationOrgUnitId("org-a")
                .build();
        Assignment roleAssignment = Assignment.builder()
                .id(101L)
                .assignmentId("role-assignment")
                .roleRef(20L)
                .resourceRef(1L)
                .applicationResourceLocationOrgUnitId("org-a")
                .build();
        Assignment deviceAssignment = Assignment.builder()
                .id(102L)
                .assignmentId("device-assignment")
                .deviceGroupRef(30L)
                .entraGroupId(UUID.randomUUID())
                .resourceRef(1L)
                .applicationResourceLocationOrgUnitId("org-b")
                .build();

        UUID resourceEntraId = UUID.randomUUID();
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("resource-1")
                .identityProviderGroupObjectId(resourceEntraId)
                .assignments(new HashSet<>(Set.of(userAssignment, roleAssignment, deviceAssignment)))
                .build();

        when(userEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE))
                .thenReturn(2L);
        when(deviceEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE))
                .thenReturn(3L);
        when(flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-a", MembershipStatus.ACTIVE))
                .thenReturn(2L);
        when(flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-b", MembershipStatus.ACTIVE))
                .thenReturn(0L);
        when(flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-c", MembershipStatus.ACTIVE))
                .thenReturn(0L);
        when(flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-a", MembershipStatus.ACTIVE))
                .thenReturn(0L);
        when(flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-b", MembershipStatus.ACTIVE))
                .thenReturn(3L);
        when(flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(1L, "org-c", MembershipStatus.ACTIVE))
                .thenReturn(0L);
        when(applicationResourceLocationRepository.findByApplicationResourceId(1L))
                .thenReturn(List.of(locationA, locationB, unusedLocation));

        resourceCountService.updateNumberOfLicenses(resource);

        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(5L);
        assertThat(locationA.getNumberOfResourcesAssigned()).isEqualTo(2L);
        assertThat(locationB.getNumberOfResourcesAssigned()).isEqualTo(3L);
        assertThat(unusedLocation.getNumberOfResourcesAssigned()).isZero();
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(locationA, resource);
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(locationB, resource);
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(unusedLocation, resource);
    }
}
