package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.cache.FintCache;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
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
        ConcurrentMessageListenerContainer<String, Membership> container = entityConsumerFactoryService.createFactory(
                        Membership.class,
                        this::processMemberships)
                .createContainer(EntityTopicNameParameters.builder()
                                         .resource("role-catalog-membership")
                                         .build());
        container.setConcurrency(5);
        return container;
    }

    @Async
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
/*
TODO: This check is not correctly implemented. It should check if the incoming membership is different from the existing one and if the status is inactive.
https://grafana.flais.io/explore?schemaVersion=1&panes=%7B%22j24%22:%7B%22datasource%22:%22-PC3f4d4z%22,%22queries%22:%5B%7B%22refId%22:%22A%22,%22expr%22:%22%7Bnamespace%3D%5C%22fintlabs-no%5C%22,%20app%3D%5C%22fint-kontroll-assignment-catalog%5C%22%7D%20%7C~%601179%60%22,%22queryType%22:%22range%22,%22datasource%22:%7B%22type%22:%22loki%22,%22uid%22:%22-PC3f4d4z%22%7D,%22editorMode%22:%22code%22%7D,%7B%22refId%22:%22B%22,%22expr%22:%22%7Bnamespace%3D%5C%22fintlabs-no%5C%22,%20app%3D%5C%22fint-kontroll-role-catalog%5C%22%7D%20%7C~%60Membership%20already%20exist%20but%20is%20different%20from%20incoming%60%22,%22queryType%22:%22range%22,%22datasource%22:%7B%22type%22:%22loki%22,%22uid%22:%22-PC3f4d4z%22%7D,%22editorMode%22:%22code%22,%22hide%22:true%7D%5D,%22range%22:%7B%22from%22:%22now-30m%22,%22to%22:%22now%22%7D%7D%7D&orgId=1

 */
    private boolean shouldDeactivateMembership(Membership incomingMembership, Membership existingMembership) {
        String existingStatus = existingMembership.getMemberStatus() != null ? existingMembership.getMemberStatus() : "active";
        log.info("Checking if membership {} should be deactivated, existing status {}, incoming status {}",
                existingMembership.getId(),
                existingStatus,
                incomingMembership.getMemberStatus());

        log.info("!existingMembership.equals(incomingMembership): {}, " +
                        "!existingStatus.equalsIgnoreCase(incomingMembership.getMemberStatus()): {}, " +
                        "incomingMembership.getMemberStatus() != null: {}, " +
                        "incomingMembership.getMemberStatus().equalsIgnoreCase(\"inactive\"): {}",
                !existingMembership.equals(incomingMembership),
                !existingStatus.equalsIgnoreCase(incomingMembership.getMemberStatus()),
                incomingMembership.getMemberStatus() != null,
                incomingMembership.getMemberStatus().equalsIgnoreCase("inactive"));

        boolean shouldDeactivateMembership = !existingMembership.equals(incomingMembership) && !existingStatus.equalsIgnoreCase(incomingMembership.getMemberStatus()) &&
               incomingMembership.getMemberStatus() != null && incomingMembership.getMemberStatus().equalsIgnoreCase("inactive");

        log.info("Should deactivateMembership: {}", shouldDeactivateMembership);

        return shouldDeactivateMembership;
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
