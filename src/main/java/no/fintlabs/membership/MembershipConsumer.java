package no.fintlabs.membership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipConsumer {

    private final MembershipRepository membershipRepository;

    private final MembershipService membershipService;

    private final FintCache<String, Membership> membershipCache;


    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumerConfiguration(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        ConcurrentMessageListenerContainer<String, Membership> container = entityConsumerFactoryService.createFactory(
                        Membership.class,
                        this::processMemberships)
                .createContainer(EntityTopicNameParameters.builder()
                        .resource("role-catalog-membership")
                        .build());
        return container;
    }

    void processMemberships(ConsumerRecord<String, Membership> consumerRecord) {
        Membership incomingMembership = consumerRecord.value();
        if (incomingMembership == null) {
            handleTombstone(consumerRecord.key());
            return;
        }
        membershipCache.getOptional(incomingMembership.getId())
                .ifPresentOrElse(
                        cachedMembership -> handleCachedMembership(cachedMembership, incomingMembership)
                        , () -> handleMembership(incomingMembership));
    }

    private void handleTombstone(String key) {
        log.info("Received tombstone for membership with id {}", key);
        membershipRepository.findById(key).ifPresent(m ->
                {
                    membershipCache.remove(key);
                    membershipService.deactivateFlattenedAssignmentsForMembership(m);
                    membershipRepository.deleteById(key);
                }
        );

    }

    private void handleCachedMembership(Membership cachedMembership, Membership incomingMembership) {
        if (!Objects.equals(cachedMembership.getMemberStatus(), incomingMembership.getMemberStatus()) || !Objects.equals(cachedMembership.getIdentityProviderUserObjectId(), incomingMembership.getIdentityProviderUserObjectId())) {
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
        String existingMemberShipStatus = existingMembership.getMemberStatus() != null ? existingMembership.getMemberStatus() : "ACTIVE";

        Membership savedMembership = updateExistingMembership(existingMembership, incomingMembership);

        if (shouldDeactivateFlattenedAssignmentsForMembership(existingMemberShipStatus, existingMembership)) {
            membershipService.deactivateFlattenedAssignmentsForMembership(savedMembership);
        } else {
            membershipService.syncAssignmentsForMembership(savedMembership);
        }
    }

    private boolean shouldDeactivateFlattenedAssignmentsForMembership(String existingMemberShipStatus, Membership incomingMembership) {

        log.info("Checking if flattened assignments for membership {} should be deactivated, existing status {}, incoming status {}",
                incomingMembership.getId(),
                existingMemberShipStatus,
                incomingMembership.getMemberStatus());

        boolean shouldDeactivateFlattenedAssignments = incomingMembership.getMemberStatus() != null
                && !existingMemberShipStatus.equalsIgnoreCase(incomingMembership.getMemberStatus())
                && incomingMembership.getMemberStatus().equalsIgnoreCase("inactive");

        log.info("Flattened assignments for membership {} should {}be deactivated",
                incomingMembership.getId(),
                shouldDeactivateFlattenedAssignments ? "" : "not "
        );

        return shouldDeactivateFlattenedAssignments;
    }

    private Membership saveNewMembership(Membership incomingMembership) {
        log.info("Incoming membership does not exist. Saving it, id {}, status {}", incomingMembership.getId(), incomingMembership.getMemberStatus());
        Membership savedMembership = membershipRepository.saveAndFlush(incomingMembership);
        membershipCache.put(savedMembership.getId(), savedMembership);
        logCacheSize();
        return savedMembership;
    }

    private Membership updateExistingMembership(Membership existingMembership, Membership incomingMembership) {
        Membership membership;

        if (!existingMembership.equals(incomingMembership)) {
            log.info("Membership already exist but is different from incoming. Saving incoming: roleId {}, memberId {}, id {}, status {}",
                    incomingMembership.getRoleId(),
                    incomingMembership.getMemberId(),
                    incomingMembership.getId(),
                    incomingMembership.getMemberStatus());
            membership = membershipRepository.saveAndFlush(mapIncomingMembershipToExistingMembership(incomingMembership, existingMembership));
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

    private Membership mapIncomingMembershipToExistingMembership(Membership incomingMembership, Membership existingMembership) {
        existingMembership.setMemberStatus(incomingMembership.getMemberStatus());
        existingMembership.setMemberStatusChanged(incomingMembership.getMemberStatusChanged());
        return existingMembership;
    }

}
