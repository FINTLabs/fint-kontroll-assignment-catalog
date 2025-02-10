package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class LicenceEnforcementService {
    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;

    public LicenceEnforcementService(ResourceRepository resourceRepository, ApplicationResourceLocationRepository applicationResourceLocationRepository) {
        this.resourceRepository = resourceRepository;
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
    }

    public void updateAssignedLicenses(Assignment assignment, Long resourceRef) {
        String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
        Long requestedNumberOfLicences;
        if (userOrGroup.equals("group")) {
           //Missing noOfMembers in role
            log.info("Group contains x members");

        }


        Resource resource = resourceRepository.findById(resourceRef).orElse(null);
        if (resource == null) {
            log.info("No resource found for ref {}", resourceRef);
            return;
        }
        if (resource.getNumberOfResourcesAssigned() + 1L > resource.getResourceLimit() && isHardStop(resource)) {
            log.info("Resource limit exceeded for ref {}", resourceRef);
            return;
        }


        ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationRepository
                .findByResourceIdAndOrgUnitId(resource.getResourceId(), assignment.getResourceConsumerOrgUnitId()).orElse(null);
        if (applicationResourceLocation == null) {
            log.info("No application resource found for ref {}", resourceRef);
            return;
        }
        if (applicationResourceLocation.getNumberOfResourcesAssigned() + 1L > applicationResourceLocation.getResourceLimit() && isHardStop(resource)) {
            log.info("Application resource limit exceeded for ref {}", resourceRef);
            return;
        }


        applicationResourceLocation.setNumberOfResourcesAssigned(applicationResourceLocation.getNumberOfResourcesAssigned() + 1);
        applicationResourceLocationRepository.save(applicationResourceLocation);




    }

    private boolean isResourceLimitExceeded()

    private boolean isHardStop(Resource resource) {
        return Objects.equals(resource.getLicenseEnforcement(), Handhevingstype.HARDSTOP.name());
    }

}
