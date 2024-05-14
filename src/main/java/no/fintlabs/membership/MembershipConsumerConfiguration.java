package no.fintlabs.membership;

import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleService;
import no.fintlabs.user.User;
import no.fintlabs.user.UserService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Configuration
public class MembershipConsumerConfiguration {
    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumer(
            MembershipService membershipService,
            EntityConsumerFactoryService entityConsumerFactoryService
    ){
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("role-catalog-membership")
                .build();

        return entityConsumerFactoryService.createFactory(
                        Membership.class,
                        (ConsumerRecord<String,Membership> consumerRecord)
                                -> membershipService.save(consumerRecord.value()))
                .createContainer(entityTopicNameParameters);
    }
}
