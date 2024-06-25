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


                            Optional<Membership> existingMemberOptional = membershipRepository.findById(incomingMembership.getId());

                            if (existingMemberOptional.isPresent()) {
                                Membership existingMembership = existingMemberOptional.get();
                                log.info("Processing incoming membership: {}, existing: {}", incomingMembership, existingMembership);

                                if (!existingMembership.equals(incomingMembership)) {
                                    log.info("Membership {} already exists but is different from the incoming membership. Saving it.", incomingMembership.getId());
                                    membershipRepository.save(incomingMembership);
                                } else {
                                    log.info("Membership {} already exists and is equal to the incoming membership. Skipping.", incomingMembership.getId());
                                }
                            } else {
                                log.info("Incoming membership with id {} does not exist in the database. Saving it.", incomingMembership.getId());
                                membershipRepository.save(incomingMembership);
                            }
                        }
                )
                .createContainer(entityTopicNameParameters);
    }
}
