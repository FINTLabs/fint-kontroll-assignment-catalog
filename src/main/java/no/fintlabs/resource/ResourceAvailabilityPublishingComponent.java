package no.fintlabs.resource;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResourceAvailabilityPublishingComponent {
    private final ResourceAvailabilityProducerService resourceAvailabilityProducerService;

    public ResourceAvailabilityPublishingComponent(
            ResourceAvailabilityProducerService resourceAvailabilityProducerService) {
        this.resourceAvailabilityProducerService = resourceAvailabilityProducerService;
    }



}
