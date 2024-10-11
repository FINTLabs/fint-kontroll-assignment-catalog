package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.consuming.ListenerConfiguration;
import no.fintlabs.kafka.consuming.ParameterizedListenerContainerFactoryService;
import no.fintlabs.kafka.topic.name.EntityTopicNameParameters;
import no.fintlabs.kafka.topic.name.TopicNamePrefixParameters;
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
            ParameterizedListenerContainerFactoryService parameterizedListenerContainerFactoryService
    ) {

        return parameterizedListenerContainerFactoryService.createRecordListenerContainerFactory(
                        Resource.class,
                        this::processResource,
                        ListenerConfiguration.builder()
                                .seekingOffsetResetOnAssignment(false)
                                .maxPollRecords(100)
                                .build())
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resourceName("resource-group")
                                         .topicNamePrefixParameters(TopicNamePrefixParameters.builder()
                                                                            .orgIdApplicationDefault()
                                                                            .domainContextApplicationDefault()
                                                                            .build())
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
