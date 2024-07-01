package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MembershipConsumer {

    private final MembershipRepository membershipRepository;

    private final MembershipService membershipService;

    private final FintCache<String, Membership> membershipCache;

    public MembershipConsumer(MembershipRepository membershipRepository, MembershipService membershipService, FintCache<String, Membership> membershipCache) {
        this.membershipRepository = membershipRepository;
        this.membershipCache = membershipCache;
        this.membershipService = membershipService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        return entityConsumerFactoryService.createFactory(
                        Membership.class,
                        this::processMemberships)
                .createContainer(EntityTopicNameParameters.builder()
                                         .resource("role-catalog-membership")
                                         .build());
    }

    private void processMemberships(ConsumerRecord<String, Membership> consumerRecord) {
        Membership incomingMembership = consumerRecord.value();

        membershipCache.getOptional(incomingMembership.getId())
                .ifPresentOrElse(
                        cachedMembership -> {
                            if (!cachedMembership.equals(incomingMembership)) {
                                process(incomingMembership);
                            }
                        }
                        , () -> process(incomingMembership));

    }

    private void process(Membership incomingMembership) {
        membershipRepository.findById(incomingMembership.getId())
                .ifPresentOrElse(
                        existingMembership -> {
                            Membership savedMembership = processExistingMembership(existingMembership, incomingMembership);
                            membershipCache.put(savedMembership.getId(), savedMembership);
                            log.info("Membership cache size is now {}", membershipCache.getAll().size());
                            membershipService.processAssignmentsForMembership(savedMembership);
                        },
                        () -> {
                            Membership savedMembership = processNewMembership(incomingMembership);
                            membershipCache.put(savedMembership.getId(), savedMembership);
                            log.info("Membership cache size is now {}", membershipCache.getAll().size());
                            membershipService.processAssignmentsForMembership(savedMembership);
                        });
    }

    private Membership processNewMembership(Membership incomingMembership) {
        log.info("Incoming membership does not exist. Saving it, roleId {}, memberId {}, id {}", incomingMembership.getRoleId(), incomingMembership.getMemberId(), incomingMembership.getId());
        return membershipRepository.save(incomingMembership);
    }

    private Membership processExistingMembership(Membership existingMembership, Membership incomingMembership) {
        if (!existingMembership.equals(incomingMembership)) {
            log.info("Membership already exist but is different from incoming. Saving it, roleId {}, memberId {}, id {}", incomingMembership.getRoleId(), incomingMembership.getMemberId(), incomingMembership.getId());
            return membershipRepository.save(incomingMembership);
        } else {
            return existingMembership;
        }
    }


}
