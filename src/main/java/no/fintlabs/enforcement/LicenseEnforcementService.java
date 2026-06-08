package no.fintlabs.enforcement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentRepository;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LicenseEnforcementService {

    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;
    private final AssignmentRepository assignmentRepository;
    private final UserEntraMembershipRepository userEntraMembershipRepository;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;

    @Value("${fint.kontroll.assignment-catalog.license-enforcement.hardstop-enable:true}")
    private boolean hardstopEnabled;

    public void updateAssignedResourcesOnDeviceGroupUpdate(DeviceGroup deviceGroup) {
        List<Assignment> assignments = getAssignmentsByDeviceGroup(deviceGroup.getId());
        if (assignments.isEmpty()) {
            log.info("No assignment found for device group {} with id : {}", deviceGroup.getName(), deviceGroup.getId());
        }
        assignments.stream()
                .map(Assignment::getResourceRef)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::recalculateAssignedResourcesForResource);
    }

    private boolean isHardStop(Resource resource) {
        return hardstopEnabled && Objects.equals(resource.getLicenseEnforcement(), Handhevingstype.HARDSTOP.name());
    }

    private List<Assignment> getAssignmentsByRole(Long roleId) {
        return assignmentRepository.findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(roleId);
    }

    private List<Assignment> getAssignmentsByDeviceGroup(Long deviceGroupId) {
        return assignmentRepository.findAssignmentsByDeviceGroupRefAndAssignmentRemovedDateIsNull(deviceGroupId);
    }

    public boolean recalculateAssignmentsForRole(Role role) {
        List<Assignment> assignments = getAssignmentsByRole(role.getId());
        if (assignments.isEmpty()) {
            log.info("No assignment found for role {} with id : {}", role.getRoleName(), role.getId());
        }
        assignments.stream()
                .map(Assignment::getResourceRef)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::recalculateAssignedResourcesForResource);
        return true;
    }

    public boolean recalculateAssignedResources(Assignment assignment) {
        if (assignment == null || assignment.getResourceRef() == null) {
            log.warn("Cannot recalculate assigned resources without resourceRef");
            return false;
        }

        return recalculateAssignedResourcesForResource(assignment.getResourceRef());
    }

    public boolean recalculateAssignedResourcesForResource(Long resourceId) {
        if (resourceId == null) {
            log.warn("Cannot recalculate assigned resources without resource id");
            return false;
        }

        Resource resource = resourceRepository.lockByResourceId(resourceId);
        List<ApplicationResourceLocation> locations = applicationResourceLocationRepository.lockByApplicationResourceId(resourceId);
        AssignedResourceCounts counts = countAssignedResources(resource, locations);

        if (isHardStop(resource) && exceedsLimits(resource, counts)) {
            return false;
        }

        persistCounts(resource, counts);
        return true;
    }

    private AssignedResourceCounts countAssignedResources(Resource resource, List<ApplicationResourceLocation> locations) {
        return new AssignedResourceCounts(
                countActiveMemberships(resource.getIdentityProviderGroupObjectId()),
                locations.stream()
                        .map(location -> new ApplicationResourceLocationCount(
                                location,
                                countActiveMembershipsForLocation(resource.getId(), location.getOrgUnitId())
                        ))
                        .toList()
        );
    }

    private boolean exceedsLimits(Resource resource, AssignedResourceCounts counts) {
        if (counts.resourceCount() > resourceLimit(resource)) {
            log.info("Resource limit exceeded for ref {}", resource.getId());
            return true;
        }
        for (ApplicationResourceLocationCount locationCount : counts.locationCounts()) {
            if (locationCount.count() > locationLimit(locationCount.location())) {
                log.info("Application resource limit exceeded for ref {} and orgUnitId {}",
                        resource.getId(), locationCount.location().getOrgUnitId());
                return true;
            }
        }
        return false;
    }

    private void persistCounts(Resource resource, AssignedResourceCounts counts) {
        resource.setNumberOfResourcesAssigned(counts.resourceCount());

        log.info("Total assigned resources for resource {} has been updated to {}",
                resource.getResourceId(), counts.resourceCount());

        counts.locationCounts().forEach(locationCount -> {
            ApplicationResourceLocation location = locationCount.location();
            location.setNumberOfResourcesAssigned(locationCount.count());
            applicationResourceLocationRepository.save(location);
            resourceAvailabilityPublishingComponent.updateResourceAvailability(location, resource);
        });
        resourceRepository.save(resource);
    }

    private long resourceLimit(Resource resource) {
        return resource.getResourceLimit() == null ? 0L : resource.getResourceLimit();
    }

    private long locationLimit(ApplicationResourceLocation location) {
        return location.getResourceLimit() == null ? 0L : location.getResourceLimit();
    }

    private long countActiveMemberships(UUID resourceEntraId) {
        if (resourceEntraId == null) {
            return 0L;
        }

        return userEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE)
                + deviceEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE);
    }

    private long countActiveMembershipsForLocation(Long resourceId, String orgUnitId) {
        return flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(
                resourceId,
                orgUnitId,
                MembershipStatus.ACTIVE)
                + flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(
                resourceId,
                orgUnitId,
                MembershipStatus.ACTIVE);
    }

    private record AssignedResourceCounts(long resourceCount, List<ApplicationResourceLocationCount> locationCounts) {
    }

    private record ApplicationResourceLocationCount(ApplicationResourceLocation location, long count) {
    }
}
