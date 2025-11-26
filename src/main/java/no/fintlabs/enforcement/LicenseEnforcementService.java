package no.fintlabs.enforcement;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.LicenseCounter;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LicenseEnforcementService {

    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final RoleRepository roleRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;
    private final AssignmentRepository assignmentRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    @Value("${fint.kontroll.assignment-catalog.license-enforcement.hardstop-enable:true}")
    private boolean hardstopEnabled;

    public boolean incrementAssignedLicensesWhenNewAssignment(Assignment assignment) {
        long requestedNumberOfLicences = calculateRequestedLicenses(assignment);
        return updateAssignedLicense(assignment, requestedNumberOfLicences);
    }

    public boolean decreaseAssignedResourcesWhenAssignmentRemoved(Assignment assignment) {
        long requestedNumberOfLicencesRemoval = calculateRequestedLicenses(assignment);
        return updateAssignedLicense(assignment, -requestedNumberOfLicencesRemoval);
    }

    private long calculateRequestedLicenses(Assignment assignment) {
        if (assignment.isGroupAssignment()) {
            return roleRepository.findById(assignment.getRoleRef())
                    .map(Role::getNoOfMembers)
                    .orElse(0L);
        } else if (assignment.isDeviceGroupAssignment()) {
           return deviceGroupRepository.findById(assignment.getDeviceGroupRef()).map(DeviceGroup::getNoOfMembers).orElse(0L);
        }
        else return 1L;
    }

    public boolean removeAllAssignedResourcesForRole(Role inActiveRole, Long noOfMembersExistingRole) {
        List<Assignment> assignments = getAssignmentsByRole(inActiveRole.getId());
        Long difference = -noOfMembersExistingRole;

        for (Assignment assignment : assignments) {
            updateAssignedLicense(assignment, difference);
        }

        return true;
    }


    public boolean updateAssignedResourcesWhenChangesInRole(Role role, Long existingNoOfMembers) {

        List<Assignment> assignments = getAssignmentsByRole(role.getId());
        if (assignments.isEmpty()) {
            log.info("No assignment found for role {} with id : {}", role.getRoleName(), role.getId());
        }
        Long difference = role.getNoOfMembers() - existingNoOfMembers;

        for (Assignment assignment : assignments) {
            updateAssignedLicense(assignment, difference);
        }

        return true;
    }

    public void updateAssignedResourcesOnDeviceGroupUpdate(DeviceGroup deviceGroup, Long delta) {

        List<Assignment> assignments = getAssignmentsByDeviceGroup(deviceGroup.getId());
        if (assignments.isEmpty()) {
            log.info("No assignment found for device group {} with id : {}", deviceGroup.getName(), deviceGroup.getId());
        }

        for (Assignment assignment : assignments) {
            updateAssignedLicense(assignment, delta);
        }
    }

    public boolean updateAssignedLicense(Assignment assignment, Long requestedNumberOfLicences) {
        if (requestedNumberOfLicences == null || requestedNumberOfLicences == 0) return true;

        Resource resource = resourceRepository.lockByResourceId(assignment.getResourceRef());

        ApplicationResourceLocation applicationResourceLocation = lockApplicationResourceLocation(assignment.getResourceRef(), assignment.getApplicationResourceLocationOrgUnitId()).orElse(null);
        if (applicationResourceLocation == null) {
            return false;
        }


        LicenseCounter licenseCounter = getLicenseCounters(applicationResourceLocation, resource);


        if (isHardStop(resource)) {
            if (licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences > licenseCounter.getApplicationResourceResourceLimit()) {
                log.info("Application resource limit exceeded for ref {}", assignment.getResourceRef());
                return false;
            }
            if (licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences > licenseCounter.getResourceResourceLimit()) {
                log.info("Resource limit exceeded for ref {}", assignment.getResourceRef());
                return false;
            }
        }

        applicationResourceLocation.setNumberOfResourcesAssigned(Math.max(licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences, 0));
        resource.setNumberOfResourcesAssigned(Math.max(licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences, 0));
        log.info("Total assign resources for resource {} has been updated to {}",
                resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences);
        applicationResourceLocationRepository.save(applicationResourceLocation);
        resourceRepository.save(resource);

        resourceAvailabilityPublishingComponent.updateResourceAvailability(applicationResourceLocation, resource);

        return true;

    }


    public boolean isHardStop(Resource resource) {
        if (hardstopEnabled) {
            return Objects.equals(resource.getLicenseEnforcement(), Handhevingstype.HARDSTOP.name());
        }
        return false;
    }

    public Optional<ApplicationResourceLocation> getApplicationResourceLocation(Assignment assignment) {
        List<ApplicationResourceLocation> locations = applicationResourceLocationRepository.findByApplicationResourceIdAndOrgUnitId(
                assignment.getResourceRef(), assignment.getApplicationResourceLocationOrgUnitId());
        return getOnlyOne(locations, assignment.getResourceRef());
    }

    public Optional<ApplicationResourceLocation> lockApplicationResourceLocation(Long resourceId, String orgUnitId) {
        List<ApplicationResourceLocation> locations = applicationResourceLocationRepository.lockByResourceAndOrgUnit(resourceId, orgUnitId);
        return getOnlyOne(locations, resourceId);

    }

    public Optional<ApplicationResourceLocation> getOnlyOne(List<ApplicationResourceLocation> locations, Long resourceId) {
        if (locations.size() > 1) {
            log.warn("Found multiple applicationResourceLocation Object for orgId: {} with resourceId: {}",
                    locations.getFirst().getOrgUnitId(), resourceId.toString());
            return Optional.empty();
        } else if (locations.isEmpty()) {
            log.warn("No applicationsResourceLocation object found for resource with resourceId: {}", resourceId.toString());
            return Optional.empty();
        }
        return Optional.of(locations.getFirst());
    }


    public LicenseCounter getLicenseCounters(ApplicationResourceLocation applicationResourceLocation, Resource resource) {

        return LicenseCounter.builder()
                .numberOfResourcesAssignedToApplicationResourceLocation(applicationResourceLocation.getNumberOfResourcesAssigned()
                        == null ? 0L : applicationResourceLocation.getNumberOfResourcesAssigned())
                .applicationResourceResourceLimit(applicationResourceLocation.getResourceLimit()
                        == null ? 0L : applicationResourceLocation.getResourceLimit())
                .numberOfResourcesAssignedToResource(resource.getNumberOfResourcesAssigned()
                        == null ? 0L : resource.getNumberOfResourcesAssigned())
                .resourceResourceLimit(resource.getResourceLimit() == null ? 0L : resource.getResourceLimit())
                .build();
    }

    public List<Assignment> getAssignmentsByRole(Long roleId) {
        return assignmentRepository.findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(roleId);
    }

    public List<Assignment> getAssignmentsByDeviceGroup(Long deviceGroupId) {
        return assignmentRepository.findAssignmentsByDeviceGroupRefAndAssignmentRemovedDateIsNull(deviceGroupId);
    }

}

