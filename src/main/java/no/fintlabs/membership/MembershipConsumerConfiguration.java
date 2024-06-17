package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.Optional;

@Slf4j
@Configuration
public class MembershipConsumerConfiguration {

    private final MembershipRepository membershipRepository;

    public MembershipConsumerConfiguration(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        EntityTopicNameParameters entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("role-catalog-membership")
                .build();

        return entityConsumerFactoryService.createFactory(
                        Membership.class,
                        (ConsumerRecord<String, Membership> consumerRecord) -> {
                            Membership incomingMembership = consumerRecord.value();

                            log.info("Processing membership: {}", incomingMembership.getId());

                            Optional<Membership> existingMemberOptional = membershipRepository.findById(incomingMembership.getId());

                            if (existingMemberOptional.isPresent()) {
                                Membership existingRole = existingMemberOptional.get();
                                if (!existingRole.equals(incomingMembership)) {
                                    membershipRepository.save(incomingMembership);
                                } else {
                                    log.info("Membership {} already exists and is equal to the incoming membership. Skipping.", incomingMembership.getId());
                                }
                            } else {
                                membershipRepository.save(incomingMembership);
                            }
                        }
                )
                .createContainer(entityTopicNameParameters);
    }
}
