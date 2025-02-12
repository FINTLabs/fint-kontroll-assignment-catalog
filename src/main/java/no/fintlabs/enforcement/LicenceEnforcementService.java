package no.fintlabs.enforcement;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class LicenceEnforcementService {
    private final ResourceRepository resourceRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final RoleRepository roleRepository;

    public LicenceEnforcementService(ResourceRepository resourceRepository, ApplicationResourceLocationRepository applicationResourceLocationRepository, RoleRepository roleRepository) {
        this.resourceRepository = resourceRepository;
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
        this.roleRepository = roleRepository;
    }

    public boolean updateAssignedLicenses(Assignment assignment, Long resourceRef) {
        String userOrGroup = assignment.getUserRef() != null ? "user" : "group";
        Long requestedNumberOfLicences;
        if (userOrGroup.equals("group")) {
            requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L);
        } else { requestedNumberOfLicences = 1L;}

        Resource resource = resourceRepository.findById(resourceRef).orElse(null);
        if (resource == null) {
            log.info("No resource found for ref {}", resourceRef);
            return false;
        }
        ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resource.getId(), assignment.getResourceConsumerOrgUnitId()).orElse(null);
        if (applicationResourceLocation == null) {
            log.info("No application resource found for ref {}", resourceRef);
            return false;
        }

        Long numberOfResourcesAssignedToApplicationResourceLocation = applicationResourceLocation.getNumberOfResourcesAssigned() == null ? 0L : applicationResourceLocation.getNumberOfResourcesAssigned();
        Long applicationResourceResourceLimit = applicationResourceLocation.getResourceLimit() == null ? 0L : applicationResourceLocation.getResourceLimit();
        Long numberOfResourcesAssignedToResource = resource.getNumberOfResourcesAssigned() == null ? 0L : resource.getNumberOfResourcesAssigned();
        Long resourceResourceLimit = resource.getResourceLimit() == null ? 0L : resource.getResourceLimit();

        if (numberOfResourcesAssignedToApplicationResourceLocation + requestedNumberOfLicences > applicationResourceResourceLimit && isHardStop(resource)) {
            log.info("Application resource limit exceeded for ref {}", resourceRef);
            return false;
        }

        if (numberOfResourcesAssignedToResource + requestedNumberOfLicences > resourceResourceLimit && isHardStop(resource)) {
            log.info("Resource limit exceeded for ref {}", resourceRef);
            return false;
        }

        applicationResourceLocation.setNumberOfResourcesAssigned(numberOfResourcesAssignedToApplicationResourceLocation + requestedNumberOfLicences);
        applicationResourceLocationRepository.save(applicationResourceLocation);

        resource.setNumberOfResourcesAssigned(numberOfResourcesAssignedToResource + requestedNumberOfLicences);
        resourceRepository.save(resource);

        return true;
    }


    private boolean isHardStop(Resource resource) {
        return Objects.equals(resource.getLicenseEnforcement(), Handhevingstype.HARDSTOP.name());
    }

}
