package no.fintlabs.enforcement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ResourceCountService {

    private final RoleRepository roleRepository;
    private final LicenseEnforcementService licenseEnforcementService;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;


    public void updateNumberOfLicenses(Resource resource) {
        log.info("Updating assigned resources for {} : {}", resource.getResourceId(), resource.getResourceName());
        AtomicLong totalNumberOfAssignedLicences = new AtomicLong(0L);
        List<Assignment> assignments = resource.getAssignments().stream()
                .filter(assignment -> assignment.getAssignmentRemovedDate() == null)
                .toList();
        log.info("Resource {} has {} assignments", resource.getResourceId(), assignments.size());
        java.util.Map<ApplicationResourceLocation, Long> byLocation = new java.util.HashMap<>();

        assignments.forEach(assignment -> {
            log.info("Update assigned resources for asignment {}", assignment.getAssignmentId());
            long requestedNumberOfLicences;
            if (assignment.isGroupAssignment()) {
                log.info("Processing assigned licences for group {}", assignment.getRoleRef());
                requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef()).map(Role::getNoOfMembers).orElse(0L).intValue();
                log.info("Number of licences assigned to group {}", requestedNumberOfLicences);
            } else {
                requestedNumberOfLicences = 1;
                log.info("Processing assigned licences for user {}", assignment.getUser().getUserName());
                log.info("Number of licences assigned to user {}", requestedNumberOfLicences);
            }
            totalNumberOfAssignedLicences.addAndGet(requestedNumberOfLicences);
            Optional<ApplicationResourceLocation> applicationResourceLocationOptional = licenseEnforcementService
                    .getApplicationResourceLocation(assignment);
            if (applicationResourceLocationOptional.isPresent()) {
                ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationOptional.get();
                applicationResourceLocation.setNumberOfResourcesAssigned(requestedNumberOfLicences);
                byLocation.merge(applicationResourceLocation, requestedNumberOfLicences, Long::sum);
            }
            else {
                log.warn("No applicationResourceLocation entity found for assignment {}", assignment.getAssignmentId());
            }
        });
        resource.setNumberOfResourcesAssigned(totalNumberOfAssignedLicences.get());
        byLocation.forEach(ApplicationResourceLocation::setNumberOfResourcesAssigned);
        byLocation.keySet().forEach(loc ->
                resourceAvailabilityPublishingComponent.updateResourceAvailability(loc, resource)
        );
        log.info("Resource {} -> assigned total {}", resource.getResourceId(), totalNumberOfAssignedLicences);
    }
}
