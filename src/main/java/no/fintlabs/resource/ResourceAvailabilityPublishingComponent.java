package no.fintlabs.resource;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResourceAvailabilityPublishingComponent {
    private final ResourceAvailabilityProducerService resourceAvailabilityProducerService;

    public ResourceAvailabilityPublishingComponent(
            ResourceAvailabilityProducerService resourceAvailabilityProducerService) {
        this.resourceAvailabilityProducerService = resourceAvailabilityProducerService;
    }


    public void updateResourceAvailability(ApplicationResourceLocation applicationResourceLocation, Resource resource) {

        ResourceConsumerAssignmentDTO resourceConsumerAssignmentDTO = ResourceConsumerAssignmentDTO.builder()
                .orgUnitId(applicationResourceLocation.getOrgUnitId())
                .assignedResources(applicationResourceLocation.getNumberOfResourcesAssigned())
                .build();

        ResourceAvailabilityDTO resourceAvailabilityDTO = ResourceAvailabilityDTO.builder()
                .resourceId(resource.getResourceId())
                .assignedResources(resource.getNumberOfResourcesAssigned())
                .resourceConsumerAssignmentDTO(resourceConsumerAssignmentDTO)
                .build();

        resourceAvailabilityProducerService.publish(resourceAvailabilityDTO);
    }
}
