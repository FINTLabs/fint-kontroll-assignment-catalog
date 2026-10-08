package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCountServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

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
        verify(applicationResourceLocationRepository).saveAll(List.of(location));
        verify(resourceAvailabilityPublishingComponent).updateResourceAvailability(location, resource);
    }

    @Test
    void updateNumberOfLicenses_sumsUserRoleAndDeviceGroupAssignments() {
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
                .azureAdGroupId(UUID.randomUUID())
                .resourceRef(1L)
                .applicationResourceLocationOrgUnitId("org-b")
                .build();

        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("resource-1")
                .assignments(new HashSet<>(Set.of(userAssignment, roleAssignment, deviceAssignment)))
                .build();

        when(roleRepository.findById(20L)).thenReturn(Optional.of(Role.builder().id(20L).noOfMembers(3L).build()));
        when(deviceGroupRepository.findById(30L)).thenReturn(Optional.of(DeviceGroup.builder().id(30L).noOfMembers(5L).build()));
        when(licenseEnforcementService.getApplicationResourceLocation(userAssignment)).thenReturn(Optional.of(locationA));
        when(licenseEnforcementService.getApplicationResourceLocation(roleAssignment)).thenReturn(Optional.of(locationA));
        when(licenseEnforcementService.getApplicationResourceLocation(deviceAssignment)).thenReturn(Optional.of(locationB));
        when(applicationResourceLocationRepository.findByApplicationResourceId(1L))
                .thenReturn(List.of(locationA, locationB, unusedLocation));

        resourceCountService.updateNumberOfLicenses(resource);

        assertThat(resource.getNumberOfResourcesAssigned()).isEqualTo(9L);
        assertThat(locationA.getNumberOfResourcesAssigned()).isEqualTo(4L);
        assertThat(locationB.getNumberOfResourcesAssigned()).isEqualTo(5L);
        assertThat(unusedLocation.getNumberOfResourcesAssigned()).isZero();
        verify(applicationResourceLocationRepository).saveAll(List.of(locationA, locationB, unusedLocation));
    }
}
