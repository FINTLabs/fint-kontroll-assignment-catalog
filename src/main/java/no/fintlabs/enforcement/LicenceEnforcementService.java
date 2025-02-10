package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.springframework.stereotype.Service;

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
        Long assignResourcesForApplicationResource;
        Long assignResourcesForApplicationResourceLocation;

        Resource resource = resourceRepository.findById(resourceRef).orElse(null);
        if (resource == null) {
            log.info("No resource found for ref {}", resourceRef);
            return;
        }

        // Finne lisenskonsumer

        Optional<ApplicationResourceLocation> applicationResourceLocation = applicationResourceLocationRepository
                .findByResourceIdAndOrgUnitId(resource.getResourceId(), assignment.getResourceConsumerOrgUnitId());
        if (applicationResourceLocation.isPresent()) {
            applicationResourceLocation.get().get

        }


    }
}
