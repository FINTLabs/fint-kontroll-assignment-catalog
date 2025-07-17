package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResourceAvailabilityProducerService {
    private final EntityProducer<ResourceAvailabilityDTO> entityProducer;
    private final EntityTopicNameParameters topicNameParameters;

    public ResourceAvailabilityProducerService(
            EntityProducerFactory entityProducerFactory,
            EntityTopicService entityTopicService) {

        entityProducer = entityProducerFactory.createProducer(ResourceAvailabilityDTO.class);
        topicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resourceavailability")
                .build();
        entityTopicService.ensureTopic(topicNameParameters, 0);
    }

    public void publish(ResourceAvailabilityDTO resourceAvailabilityDTO) {
        String key = resourceAvailabilityDTO.getResourceId();

        entityProducer.send(
                EntityProducerRecord.<ResourceAvailabilityDTO>builder()
                        .topicNameParameters(topicNameParameters)
                        .key(key)
                        .value(resourceAvailabilityDTO)
                        .build()
        );
    }

}
