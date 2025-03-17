package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.resource.LicenseCounter;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UpdateAssignedResourcesService {
    private final ResourceRepository resourceRepository;
    private final RoleRepository roleRepository;
    private final LicenseEnforcementService licenseEnforcementService;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    public UpdateAssignedResourcesService(
            ResourceRepository resourceRepository,
            RoleRepository roleRepository,
            LicenseEnforcementService licenseEnforcementService,
            ApplicationResourceLocationRepository applicationResourceLocationRepository,
            ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent) {
        this.resourceRepository = resourceRepository;
        this.roleRepository = roleRepository;
        this.licenseEnforcementService = licenseEnforcementService;
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
        this.resourceAvailabilityPublishingComponent = resourceAvailabilityPublishingComponent;
    }

    public void updateAssignedResources() {
        List<Resource> resources = resourceRepository.findAll();

        for (Resource resource : resources) {
            List<Assignment> assignments = resource.getAssignments().stream()
                    .filter(assignment -> assignment.getAssignmentRemovedDate() != null)
                    .toList();
            for (Assignment assignment : assignments) {
                String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
                Long requestedNumberOfLicences;
                if (userOrGroup.equals("group")) {
                    requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
                } else { requestedNumberOfLicences = 1L;}

                ApplicationResourceLocation applicationResourceLocation = licenseEnforcementService
                        .getApplicationResourceLocation(assignment);

                LicenseCounter licenseCounter = licenseEnforcementService.getLicenseCounters(applicationResourceLocation,resource);

                applicationResourceLocation
                        .setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences);
                applicationResourceLocationRepository.saveAndFlush(applicationResourceLocation);
                log.info("Assigned resources for applicationResourceLocation to resource {} has been updated to {} assigned resources",
                        resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences);

                resource.setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences);
                resourceRepository.saveAndFlush(resource);
                log.info("Total assign resources for resource {} has been updated to {}",
                        resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences);

                resourceAvailabilityPublishingComponent.updateResourceAvailability(applicationResourceLocation,resource);
            }
        }
    }
}
