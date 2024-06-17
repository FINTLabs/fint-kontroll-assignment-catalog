package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class ResourceConsumerConfiguration {

    private ResourceService resourceService;

    public ResourceConsumerConfiguration(ResourceService resourceService) {
        this.resourceService = resourceService;
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
        log.info("Processing resource: {}", record.value());
        resourceService.save(record.value());
    }
}
