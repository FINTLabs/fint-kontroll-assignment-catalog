package no.fintlabs.enforcement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentRepository;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.entra.MembershipStatus;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ResourceCountService {

    private final UserEntraMembershipRepository userEntraMembershipRepository;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;
    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;


    public void updateNumberOfLicenses(Resource resource) {
        log.info("Updating assigned resources for {} : {}", resource.getResourceId(), resource.getResourceName());

        long totalNumberOfAssignedLicences = countActiveMemberships(resource.getIdentityProviderGroupObjectId());
        resource.setNumberOfResourcesAssigned(totalNumberOfAssignedLicences);

        List<ApplicationResourceLocation> locations =
                applicationResourceLocationRepository.findByApplicationResourceId(resource.getId());
        locations.forEach(location -> {
            long locationAssignedResources = countActiveMembershipsForLocation(resource.getId(), location.getOrgUnitId());
            location.setNumberOfResourcesAssigned(locationAssignedResources);
            resourceAvailabilityPublishingComponent.updateResourceAvailability(location, resource);
        });

        log.info("Resource {} -> assigned total {}", resource.getResourceId(), totalNumberOfAssignedLicences);
    }

    private long countActiveMemberships(UUID resourceEntraId) {
        if (resourceEntraId == null) {
            return 0L;
        }

        return userEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE)
                + deviceEntraMembershipRepository.countByResourceEntraIdAndMembershipStatus(resourceEntraId, MembershipStatus.ACTIVE);
    }

    private long countActiveMembershipsForLocation(Long resourceId, String orgUnitId) {
        return flattenedAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(
                resourceId,
                orgUnitId,
                MembershipStatus.ACTIVE)
                + flattenedDeviceAssignmentRepository.countDistinctMembershipsByResourceRefAndOrgUnitIdAndMembershipStatus(
                resourceId,
                orgUnitId,
                MembershipStatus.ACTIVE);
    }
}
