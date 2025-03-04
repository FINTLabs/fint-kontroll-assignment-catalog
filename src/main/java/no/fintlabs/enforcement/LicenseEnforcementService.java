package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.LicenseCounter;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class LicenseEnforcementService {
    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final RoleRepository roleRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;
    private final AssignmentRepository assignmentRepository;

    public LicenseEnforcementService(ResourceRepository resourceRepository,
                                     ApplicationResourceLocationRepository applicationResourceLocationRepository,
                                     RoleRepository roleRepository,
                                     ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent,
                                     AssignmentRepository assignmentRepository) {
        this.resourceRepository = resourceRepository;
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
        this.roleRepository = roleRepository;
        this.resourceAvailabilityPublishingComponent = resourceAvailabilityPublishingComponent;
        this.assignmentRepository = assignmentRepository;
    }

    public boolean incrementAssignedLicensesWhenNewAssignment(Assignment assignment) {
        String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
        Long requestedNumberOfLicences;
        if (userOrGroup.equals("group")) {
            requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
        } else { requestedNumberOfLicences = 1L;}

        return updateAssignedLicense(assignment, requestedNumberOfLicences);
    }

    public boolean decreaseAssignedResourcesWhenAssignmentRemoved(Assignment assignment) {
        String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
        Long requestedNumberOfLicencesRemoval;
        if (userOrGroup.equals("group")) {
            requestedNumberOfLicencesRemoval = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
        } else { requestedNumberOfLicencesRemoval = 1L;}

        return updateAssignedLicense(assignment, -requestedNumberOfLicencesRemoval);
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
        if (assignments.isEmpty()) {log.info("No assignment found for role {} with id : {}", role.getRoleName(),role.getId());}
        Long difference = role.getNoOfMembers() - existingNoOfMembers;

        for (Assignment assignment : assignments) {
            updateAssignedLicense(assignment, difference);
        }

        return true;
    }


    public boolean updateAssignedLicense(Assignment assignment,Long numberOfAssignments) {
        Resource resource = getResource(assignment.getResourceRef());
        if (resource == null) {
            return false;
        }

        ApplicationResourceLocation applicationResourceLocation = getApplicationResourceLocation(assignment);
        if (applicationResourceLocation == null) {
            return false;
        }

        LicenseCounter licenseCounter = getLicenseCounters(applicationResourceLocation,resource);

        if (licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + numberOfAssignments
                > licenseCounter.getApplicationResourceResourceLimit() && isHardStop(resource)) {
            log.info("Application resource limit exceeded for ref {}", assignment.getResourceRef());
            return false;
        }

        if (licenseCounter.getNumberOfResourcesAssignedToResource() + numberOfAssignments
                > licenseCounter.getResourceResourceLimit() && isHardStop(resource)) {
            log.info("Resource limit exceeded for ref {}", assignment.getResourceRef());
            return false;
        }

        applicationResourceLocation
                .setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + numberOfAssignments);
        applicationResourceLocationRepository.saveAndFlush(applicationResourceLocation);
        log.info("Assigned resources for applicationResourceLocation to resource {} has been updated to {} assigned resources",
                resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + numberOfAssignments);

        resource.setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToResource() + numberOfAssignments);
        resourceRepository.saveAndFlush(resource);
        log.info("Total assign resources for resource {} has been updated to {}",
                resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToResource() + numberOfAssignments);

        resourceAvailabilityPublishingComponent.updateResourceAvailability(applicationResourceLocation,resource);

        return true;
    }


    public boolean isHardStop(Resource resource) {
        return Objects.equals(resource.getLicenseEnforcement(), Handhevingstype.HARDSTOP.name());
    }

    public Resource getResource(Long resourceRef) {
        Resource resource = resourceRepository.findById(resourceRef).orElse(null);
        if (resource == null) {
            log.info("No resource found for ref {}", resourceRef);
            return null;
        }

        return resource;
    }


    public ApplicationResourceLocation getApplicationResourceLocation(Assignment assignment) {
        ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(assignment.getResourceRef(), assignment.getApplicationResourceLocationOrgUnitId()).orElse(null);
        if (applicationResourceLocation == null) {
            log.info("No application resource found for ref {}", assignment.getResourceRef());
            return null;
        }

        return applicationResourceLocation;
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
}

