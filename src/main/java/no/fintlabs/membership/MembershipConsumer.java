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

    void processMemberships(ConsumerRecord<String, Membership> consumerRecord) {
        Membership incomingMembership = consumerRecord.value();

        membershipCache.getOptional(incomingMembership.getId())
                .ifPresentOrElse(
                        cachedMembership -> handleCachedMembership(cachedMembership, incomingMembership)
                        , () -> handleMembership(incomingMembership));
    }

    private void handleCachedMembership(Membership cachedMembership, Membership incomingMembership) {
        if (!cachedMembership.equals(incomingMembership)) {
            handleMembership(incomingMembership);
        }
    }

    private void handleMembership(Membership incomingMembership) {
        membershipRepository.findById(incomingMembership.getId())
                .ifPresentOrElse(
                        existingMembership -> handleExistingMembership(incomingMembership, existingMembership),
                        () -> handleNewMembership(incomingMembership));
    }

    private void handleNewMembership(Membership incomingMembership) {
        Membership savedMembership = saveNewMembership(incomingMembership);
        membershipService.syncAssignmentsForMembership(savedMembership);
    }

    private void handleExistingMembership(Membership incomingMembership, Membership existingMembership) {
        Membership savedMembership = updateExistingMembership(existingMembership, incomingMembership);

        if (shouldDeactivateMembership(incomingMembership, existingMembership)) {
            membershipService.deactivateFlattenedAssignmentsForMembership(savedMembership);
        } else {
            membershipService.syncAssignmentsForMembership(savedMembership);
        }
    }

    private boolean shouldDeactivateMembership(Membership incomingMembership, Membership existingMembership) {
        String existingStatus = existingMembership.getMemberStatus() != null ? existingMembership.getMemberStatus() : "active";

        return !existingMembership.equals(incomingMembership) && !existingStatus.equalsIgnoreCase(incomingMembership.getMemberStatus()) &&
               incomingMembership.getMemberStatus() != null && incomingMembership.getMemberStatus().equalsIgnoreCase("inactive");
    }

    private Membership saveNewMembership(Membership incomingMembership) {
        log.info("Incoming membership does not exist. Saving it, roleId {}, memberId {}, id {}, status {}",
                incomingMembership.getRoleId(),
                incomingMembership.getMemberId(),
                incomingMembership.getId(),
                incomingMembership.getMemberStatus());
        Membership savedMembership = membershipRepository.saveAndFlush(incomingMembership);
        membershipCache.put(savedMembership.getId(), savedMembership);
        logCacheSize();
        return savedMembership;
    }

    private Membership updateExistingMembership(Membership existingMembership, Membership incomingMembership) {
        Membership membership;

        if (!existingMembership.equals(incomingMembership)) {
            log.info("Membership already exist but is different from incoming. Saving it, roleId {}, memberId {}, id {}, status {}",
                    incomingMembership.getRoleId(),
                    incomingMembership.getMemberId(),
                    incomingMembership.getId(),
                    incomingMembership.getMemberStatus());
            membership = membershipRepository.saveAndFlush(incomingMembership);
        } else {
            membership = existingMembership;
        }

        membershipCache.put(membership.getId(), membership);
        logCacheSize();
        return membership;
    }

    private void logCacheSize() {
        long cacheSize = membershipCache.getNumberOfEntries();

        if (cacheSize % 100 == 0) {
            log.info("Membership cache size is now {}", cacheSize);
        }
    }


}
