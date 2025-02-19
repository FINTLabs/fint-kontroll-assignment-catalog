package no.fintlabs.resource;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ResourceAvailabilityPublishingComponent {
    private final ResourceAvailabilityProducerService resourceAvailabilityProducerService;

    public ResourceAvailabilityPublishingComponent(
            ResourceAvailabilityProducerService resourceAvailabilityProducerService) {
        this.resourceAvailabilityProducerService = resourceAvailabilityProducerService;
    }


    public void updateResourceAvailability(ApplicationResourceLocation applicationResourceLocation, Resource resource) {

        ResourceConsumerAssignments resourceConsumerAssignment = ResourceConsumerAssignments.builder()
                .orgUnitId(applicationResourceLocation.getOrgUnitId())
                .assignedResources(applicationResourceLocation.getNumberOfResourcesAssigned())
                .build();

        ResourceAvailability resourceAvailability = ResourceAvailability.builder()
                .resourceId(resource.getResourceId())
                .assignedResources(resource.getNumberOfResourcesAssigned())
                .resourceConsumerAssignments(List.of(resourceConsumerAssignment))
                .build();

        resourceAvailabilityProducerService.publish(resourceAvailability);
    }
}
