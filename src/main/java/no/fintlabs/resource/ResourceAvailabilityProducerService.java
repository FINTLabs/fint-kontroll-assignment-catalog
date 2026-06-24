package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EntityTopicService;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResourceAvailabilityProducerService {
    private final ParameterizedTemplate<ResourceAvailabilityDTO> entityProducer;
    private final EntityTopicNameParameters topicNameParameters;

    public ResourceAvailabilityProducerService(
            ParameterizedTemplateFactory entityProducerFactory,
            EntityTopicService entityTopicService) {

        entityProducer = entityProducerFactory.createTemplate(ResourceAvailabilityDTO.class);
        topicNameParameters = KafkaEntityTopics.topicNameParameters("resourceavailability");
        entityTopicService.createOrModifyTopic(topicNameParameters, KafkaEntityTopics.compactedTopicConfiguration());
    }

    public void publish(ResourceAvailabilityDTO resourceAvailabilityDTO) {
        String key = resourceAvailabilityDTO.getResourceId();

        entityProducer.send(
                ParameterizedProducerRecord.<ResourceAvailabilityDTO>builder()
                        .topicNameParameters(topicNameParameters)
                        .key(key)
                        .value(resourceAvailabilityDTO)
                        .build()
        );
    }

}
