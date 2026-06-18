package no.fintlabs.enforcement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
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
    private final DeviceGroupRepository deviceGroupRepository;
    private final LicenseEnforcementService licenseEnforcementService;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;


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
            long requestedNumberOfLicences = calculateAssignedLicenses(assignment);
            totalNumberOfAssignedLicences.addAndGet(requestedNumberOfLicences);
            Optional<ApplicationResourceLocation> applicationResourceLocationOptional =
                    licenseEnforcementService.getApplicationResourceLocation(assignment);
            if (applicationResourceLocationOptional.isPresent()) {
                ApplicationResourceLocation applicationResourceLocation = applicationResourceLocationOptional.get();
                byLocation.merge(applicationResourceLocation, requestedNumberOfLicences, Long::sum);
            }
            else {
                log.warn("No applicationResourceLocation entity found for assignment {}", assignment.getAssignmentId());
            }

        });
        resource.setNumberOfResourcesAssigned(totalNumberOfAssignedLicences.get());
        updateApplicationResourceLocationCounts(resource, byLocation);
        log.info("Resource {} -> assigned total {}", resource.getResourceId(), totalNumberOfAssignedLicences);
    }

    private long calculateAssignedLicenses(Assignment assignment) {
        if (assignment.isGroupAssignment()) {
            log.info("Processing assigned licences for group {}", assignment.getRoleRef());
            long requestedNumberOfLicences = roleRepository.findById(assignment.getRoleRef())
                    .map(Role::getNoOfMembers)
                    .orElse(0L);
            log.info("Number of licences assigned to group {}", requestedNumberOfLicences);
            return requestedNumberOfLicences;
        }

        if (assignment.isDeviceGroupAssignment()) {
            long requestedNumberOfLicences = deviceGroupRepository.findById(assignment.getDeviceGroupRef())
                    .map(DeviceGroup::getNoOfMembers)
                    .orElse(0L);
            log.info("Processing assigned licences for device group {}", assignment.getDeviceGroupRef());
            log.info("Number of licences assigned to device group {}", requestedNumberOfLicences);
            return requestedNumberOfLicences;
        }

        log.info("Processing assigned licences for user {}", assignment.getUserRef());
        return 1L;
    }

    private void updateApplicationResourceLocationCounts(Resource resource, java.util.Map<ApplicationResourceLocation, Long> byLocation) {
        List<ApplicationResourceLocation> locations = applicationResourceLocationRepository.findByApplicationResourceId(resource.getId());
        locations.forEach(location -> location.setNumberOfResourcesAssigned(byLocation.getOrDefault(location, 0L)));
        applicationResourceLocationRepository.saveAll(locations);
        locations.forEach(location -> resourceAvailabilityPublishingComponent.updateResourceAvailability(location, resource));
    }
}
