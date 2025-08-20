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
        log.info("Start updating assignedResources initiated by api call");
        log.info("Clearing assignedResources");
        applicationResourceLocationRepository.clearNumberOfResourcesAssignedInLocations();
        resourceRepository.clearNumberOfResourcesAssignedInResources();

        List<Resource> resources = resourceRepository.findByStatusACTIVE();
        log.info("Number of ACTIVE resources to update assigned resources: {} resources", resources.size());

        for (Resource resource : resources) {
            log.info("Updating assigned resources for {} : {}", resource.getResourceId(), resource.getResourceName());
            List<Assignment> assignments = resource.getAssignments().stream()
                    .filter(assignment -> assignment.getAssignmentRemovedDate() == null)
                    .toList();
            log.info("Resource {} has {} assignments", resource.getResourceId(), assignments.size());
            for (Assignment assignment : assignments) {
                log.info("Update assigned resources for asignment {}", assignment.getAssignmentId());
                Long requestedNumberOfLicences;
                if (assignment.isGroupAssignment()) {
                    log.info("Processing assigned licences for group {}", assignment.getRoleRef());
                    requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
                    log.info("Number of licences assigned to group {}", requestedNumberOfLicences);
                } else {
                    requestedNumberOfLicences = 1L;
                    log.info("Processing assigned licences for user {}", assignment.getUser().getUserName());
                    log.info("Number of licences assigned to user {}", requestedNumberOfLicences);
                }

                Optional <ApplicationResourceLocation> applicationResourceLocationOptional = licenseEnforcementService
                        .getApplicationResourceLocation(assignment);
                if (applicationResourceLocationOptional.isPresent()) {
                    ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationOptional.get();

                    Resource resourceCurrentInDB = licenseEnforcementService.getResource(resource.getId());
                    LicenseCounter licenseCounter = licenseEnforcementService.getLicenseCounters(applicationResourceLocation,resourceCurrentInDB);
                    log.info(licenseCounter.toString());



                    applicationResourceLocation
                            .setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences);
                    applicationResourceLocationRepository.saveAndFlush(applicationResourceLocation);

                    log.info("Assigned resources for applicationResourceLocation {} to resource {} has been updated to {} assigned resources", applicationResourceLocation.getOrgUnitId(),
                            resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToApplicationResourceLocation() + requestedNumberOfLicences);

                    resourceCurrentInDB.setNumberOfResourcesAssigned(licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences);
                    resourceRepository.saveAndFlush(resourceCurrentInDB);
                    log.info("Total assign resources for resource {} has been updated to {}",
                            resource.getResourceId(), licenseCounter.getNumberOfResourcesAssignedToResource() + requestedNumberOfLicences);

                    resourceAvailabilityPublishingComponent.updateResourceAvailability(applicationResourceLocation,resourceCurrentInDB);
                } else {
                    log.warn("No applicationResourceLocation entity found for assignment {}", assignment.getAssignmentId());
                }
            }
        }
    }
}
