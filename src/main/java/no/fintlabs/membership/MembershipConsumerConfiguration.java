package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class MembershipConsumerConfiguration {

    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumer(
            MembershipService membershipService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("role-catalog-membership")
                .build();

        return entityConsumerFactoryService.createFactory(
                        Membership.class,
                        (ConsumerRecord<String, Membership> consumerRecord) -> {
                            log.info("Processing membership: {}", consumerRecord.value());
                            membershipService.save(consumerRecord.value());
                        }
                )
                .createContainer(entityTopicNameParameters);
    }
}
