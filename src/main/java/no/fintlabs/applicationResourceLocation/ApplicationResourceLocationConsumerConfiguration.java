package no.fintlabs.applicationResourceLocation;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class ApplicationResourceLocationConsumerConfiguration {
    private final EntityConsumerFactoryService entityConsumerFactoryService;

    public ApplicationResourceLocationConsumerConfiguration(EntityConsumerFactoryService entityConsumerFactoryService) {
        this.entityConsumerFactoryService = entityConsumerFactoryService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ApplicationResourceLocation> applicationResourceLocationConsumer(
            FintCache<Long, ApplicationResourceLocation> applicationResourceLocationCache
    ) {

        return entityConsumerFactoryService.createFactory(
                        ApplicationResourceLocation.class,
                        consumerRecord ->
                                applicationResourceLocationCache.put(
                                        consumerRecord.value().id(),
                                        consumerRecord.value()))
                .createContainer(EntityTopicNameParameters.builder().resource("applicationresourcelocation-extended").build());
    }
}
