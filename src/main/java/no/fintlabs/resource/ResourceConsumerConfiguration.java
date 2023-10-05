package no.fintlabs.resource;

import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.role.Role;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
public class ResourceConsumerConfiguration {
    @Bean
    public ConcurrentMessageListenerContainer<String, Resource> resourceConsumer(
            ResourceService resourceService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ){
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resource-group")
                .build();

        ConcurrentMessageListenerContainer container = entityConsumerFactoryService.createFactory(
                        Resource.class,
                        (ConsumerRecord<String,Resource> consumerRecord)
                                -> resourceService.save(consumerRecord.value()))
                .createContainer(entityTopicNameParameters);

        return container;
    }
}
