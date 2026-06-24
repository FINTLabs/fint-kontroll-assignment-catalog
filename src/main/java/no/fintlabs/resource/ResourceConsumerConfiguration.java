package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Optional;

@Slf4j
@Configuration
public class ResourceConsumerConfiguration {

    private final ResourceRepository resourceRepository;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;

    public ResourceConsumerConfiguration(ResourceRepository resourceRepository,
                                         KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults) {
        this.resourceRepository = resourceRepository;
        this.kafkaConsumerConfigurationDefaults = kafkaConsumerConfigurationDefaults;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Resource> resourceConsumer(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createRecordListenerContainerFactory(
                        Resource.class,
                        this::processResource,
                        KafkaEntityTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEntityTopics.topicNameParameters("resource-group"));
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
