package no.fintlabs.applicationResourceLocation;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class ApplicationResourceLocationConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, ApplicationResourceLocation> applicationResourceLocationConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService,
            ApplicationResourceLocationService applicationResourceLocationService
    ) {

        return entityConsumerFactoryService.createFactory(
                        ApplicationResourceLocation.class,
                        (ConsumerRecord<String, ApplicationResourceLocation> consumerRecord)
                                -> applicationResourceLocationService.save(consumerRecord.value()))
                .createContainer(EntityTopicNameParameters.builder().resource("applicationresourcelocation-extended").build());
    }
}
