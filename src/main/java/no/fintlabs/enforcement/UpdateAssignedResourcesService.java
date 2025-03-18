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
import java.util.Optional;

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
        log.info("Number of resources to update assigned resources: {} resources", resources.size());

        for (Resource resource : resources) {
            log.info("Updating assigned resources for {} : {}", resource.getResourceId(), resource.getResourceName());
            List<Assignment> assignments = resource.getAssignments().stream()
                    .filter(assignment -> assignment.getAssignmentRemovedDate() == null)
                    .toList();
            log.info("Resource {} has {} assignments", resource.getResourceId(), assignments.size());
            for (Assignment assignment : assignments) {
                log.info("Update assigned resources for asignment {}", assignment.getAssignmentId());
                String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
                Long requestedNumberOfLicences;
                if (userOrGroup.equals("group")) {
                    log.info("Processing assigned licences for group {}", assignment.getRoleRef());
                    requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
                    log.info("Number of licences assigned to group {}", requestedNumberOfLicences);
                } else {
                    requestedNumberOfLicences = 1L;
                    log.info("Processing assigned licences for user {}", assignment.getUserRef());
                    log.info("Number of licences assigned to user {}", requestedNumberOfLicences);
                }

                Optional <ApplicationResourceLocation> applicationResourceLocationOptional = licenseEnforcementService
                        .getApplicationResourceLocation(assignment);
                if (applicationResourceLocationOptional.isPresent()) {
                    ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationOptional.get();
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
                } else {
                    log.warn("No application resource location found for assignment {}", assignment.getAssignmentId());
                }
            }
        }
    }
}
