package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class UserConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, User> userConsumer(
            UserService userService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("kontrolluser")
                .build();

        ConcurrentMessageListenerContainer container = entityConsumerFactoryService.createFactory(
                        KontrollUser.class,
                        (ConsumerRecord<String, KontrollUser> consumerRecord) -> {
                            log.info("Processing user: {}", consumerRecord.value());
                            userService.convertAndSaveAsUser(consumerRecord.value());
                        }
                )
                .createContainer(entityTopicNameParameters);

        return container;
    }
}
