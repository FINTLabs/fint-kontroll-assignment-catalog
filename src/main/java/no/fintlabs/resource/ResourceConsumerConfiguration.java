package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Optional;

@Slf4j
@Configuration
public class ResourceConsumerConfiguration {

    private final ResourceRepository resourceRepository;

    public ResourceConsumerConfiguration(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Resource> resourceConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createFactory(
                        Resource.class,
                        this::processResource)
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("resource-group")
                                         .build());
    }

    void processResource(ConsumerRecord<String, Resource> record) {
        Resource incomingResource = record.value();
        log.info("Processing resource: {}", incomingResource.getId());

        Optional<Resource> existingResourceOptional = resourceRepository.findById(incomingResource.getId());

        if (existingResourceOptional.isPresent()) {
            Resource existingResource = existingResourceOptional.get();
            if (!existingResource.equals(incomingResource)) {
                resourceRepository.save(incomingResource);
            } else {
                log.info("Resource {} already exists and is equal to the incoming resource. Skipping.", incomingResource.getId());
            }
        } else {
            resourceRepository.save(incomingResource);
        }
    }
}
