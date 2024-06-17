package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class RoleConsumerConfiguration {
    @Bean
    public ConcurrentMessageListenerContainer<String, Role> roleConsumer(
            RoleService roleService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ){
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("role-catalog-role")
                .build();

        ConcurrentMessageListenerContainer container = entityConsumerFactoryService.createFactory(
                        Role.class,
                        (ConsumerRecord<String,Role> consumerRecord) -> {
                            log.info("Processing role: {}", consumerRecord.value());
                            roleService.save(consumerRecord.value());
                        })
                .createContainer(entityTopicNameParameters);

        return container;
    }
}
