package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class LicenseEnforcementService {
    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final RoleRepository roleRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    public LicenseEnforcementService(ResourceRepository resourceRepository, ApplicationResourceLocationRepository applicationResourceLocationRepository, RoleRepository roleRepository, ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent) {
        this.resourceRepository = resourceRepository;
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
        this.roleRepository = roleRepository;
        this.resourceAvailabilityPublishingComponent = resourceAvailabilityPublishingComponent;
    }

    public boolean incrementAssignedLicenses(Assignment assignment, Long resourceRef) {
        String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
        Long requestedNumberOfLicences;
        if (userOrGroup.equals("group")) {
            requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
        } else { requestedNumberOfLicences = 1L;}

        Resource resource = getResource(resourceRef);
        if (resource == null) {
            return false;
        }

        ApplicationResourceLocation applicationResourceLocation = getApplicationResourceLocation(assignment);
        if (applicationResourceLocation == null) {
            return false;
        }

        Map<String,Long> licenseCounters = getLicenseCounters(applicationResourceLocation,resource);

        if (licenseCounters.get("numberOfResourcesAssignedToApplicationResourceLocation") + requestedNumberOfLicences > licenseCounters.get("applicationResourceResourceLimit") && isHardStop(resource)) {
            log.info("Application resource limit exceeded for ref {}", resourceRef);
            return false;
        }

        if (licenseCounters.get("numberOfResourcesAssignedToResource") + requestedNumberOfLicences > licenseCounters.get("resourceResourceLimit") && isHardStop(resource)) {
            log.info("Resource limit exceeded for ref {}", resourceRef);
            return false;
        }

        applicationResourceLocation.setNumberOfResourcesAssigned(licenseCounters.get("numberOfResourcesAssignedToApplicationResourceLocation") + requestedNumberOfLicences);
        applicationResourceLocationRepository.saveAndFlush(applicationResourceLocation);
        log.info("Assigned resources for applicationResourceLocation to resource {} has been updated to {} assigned resources",
                resourceRef, licenseCounters.get("numberOfResourcesAssignedToApplicationResourceLocation") + requestedNumberOfLicences);

        resource.setNumberOfResourcesAssigned(licenseCounters.get("numberOfResourcesAssignedToResource") + requestedNumberOfLicences);
        resourceRepository.saveAndFlush(resource);
        log.info("Total assign resources for resource {} has been updated to {}",
                resourceRef, licenseCounters.get("numberOfResourcesAssignedToResource") + requestedNumberOfLicences);

        resourceAvailabilityPublishingComponent.updateResourceAvailability(applicationResourceLocation,resource);

        return true;
    }


    public boolean removeAssignedLicense(Assignment assignment) {
        Resource resource = getResource(assignment.getResourceRef());
        if (resource == null) {
            return false;
        }

        ApplicationResourceLocation applicationResourceLocation = getApplicationResourceLocation(assignment);
        if (applicationResourceLocation == null) {
            return false;
        }

        Map<String,Long> licenseCounters = getLicenseCounters(applicationResourceLocation,resource);
        applicationResourceLocation
                .setNumberOfResourcesAssigned(licenseCounters.get("numberOfResourcesAssignedToApplicationResourceLocation") - 1);
        applicationResourceLocationRepository.saveAndFlush(applicationResourceLocation);
        log.info("Assigned resources for applicationResourceLocation to resource {} has been updated to {} assigned resources",
                resource.getResourceId(), licenseCounters.get("numberOfResourcesAssignedToApplicationResourceLocation") -1);
        resource.setNumberOfResourcesAssigned(licenseCounters.get("numberOfResourcesAssignedToResource") - 1);
        resourceRepository.saveAndFlush(resource);
        log.info("Total assign resources for resource {} has been updated to {}",
                resource.getResourceId(), licenseCounters.get("numberOfResourcesAssignedToResource") - 1);

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
                .findByApplicationResourceIdAndOrgUnitId(assignment.getResourceRef(), assignment.getResourceConsumerOrgUnitId()).orElse(null);
        if (applicationResourceLocation == null) {
            log.info("No application resource found for ref {}", assignment.getResourceRef());
            return null;
        }
        return applicationResourceLocation;
    }


    //TODO: benytte classe/record for å holde verdiene i stedet for Map
    public Map<String,Long> getLicenseCounters(ApplicationResourceLocation applicationResourceLocation, Resource resource) {
        Map<String,Long> counters = new HashMap<>();
        Long numberOfResourcesAssignedToApplicationResourceLocation =
                applicationResourceLocation.getNumberOfResourcesAssigned() == null ? 0L : applicationResourceLocation.getNumberOfResourcesAssigned();
        Long applicationResourceResourceLimit =
                applicationResourceLocation.getResourceLimit() == null ? 0L : applicationResourceLocation.getResourceLimit();
        Long numberOfResourcesAssignedToResource =
                resource.getNumberOfResourcesAssigned() == null ? 0L : resource.getNumberOfResourcesAssigned();
        Long resourceResourceLimit = resource.getResourceLimit() == null ? 0L : resource.getResourceLimit();

        counters.put("numberOfResourcesAssignedToApplicationResourceLocation", numberOfResourcesAssignedToApplicationResourceLocation);
        counters.put("applicationResourceResourceLimit", applicationResourceResourceLimit);
        counters.put("numberOfResourcesAssignedToResource", numberOfResourcesAssignedToResource);
        counters.put("resourceResourceLimit", resourceResourceLimit);
        return counters;

    }

}
