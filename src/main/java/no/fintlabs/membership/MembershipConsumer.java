package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
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
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssignmentService assignmentService;
    private final FintCache<String, Membership> membershipCache;

    public MembershipConsumer(MembershipRepository membershipRepository, AssignmentService assignmentService, FlattenedAssignmentService flattenedAssignmentService,
                              FintCache<String, Membership> membershipCache) {
        this.membershipRepository = membershipRepository;
        this.assignmentService = assignmentService;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.membershipCache = membershipCache;
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
                        existingMembership -> processExistingMembership(existingMembership, incomingMembership),
                        () -> processNewMembership(incomingMembership));
    }

    private void processNewMembership(Membership incomingMembership) {
        log.info("Incoming membership does not exist. Saving it, roleId {}, memberId {}, id {}", incomingMembership.getRoleId(), incomingMembership.getMemberId(), incomingMembership.getId());
        Membership savedMembership = membershipRepository.save(incomingMembership);
        processAssignmentsForMembership(savedMembership);
    }

    private void processExistingMembership(Membership existingMembership, Membership incomingMembership) {
        if (!existingMembership.equals(incomingMembership)) {
            log.info("Membership already exist but is different from incoming. Saving it, roleId {}, memberId {}, id {}", incomingMembership.getRoleId(), incomingMembership.getMemberId(), incomingMembership.getId());
            Membership savedMembership = membershipRepository.save(incomingMembership);
            processAssignmentsForMembership(savedMembership);
        } else {
            processAssignmentsForMembership(existingMembership);
        }
    }

    @Async("processAssignmentsForMembership")
    void processAssignmentsForMembership(Membership savedMembership) {
        membershipCache.put(savedMembership.getId(), savedMembership);

        if (savedMembership.getIdentityProviderUserObjectId() == null) {
            log.info("Membership does not have identityProviderUserObjectId, skipping assignment processing, roleId {}, memberId {}, id {}", savedMembership.getRoleId(), savedMembership.getMemberId(), savedMembership.getId());
            return;
        }

        log.info("Membership cache size is now {}", membershipCache.getAll().size());

        assignmentService.getAssignmentsByRole(savedMembership.getRoleId()).forEach(assignment -> {
            try {
                flattenedAssignmentService.createFlattenedAssignmentsForMembership(assignment, savedMembership.getMemberId(), savedMembership.getRoleId());
            } catch (Exception e) {
                log.error("Error processing assignments for membership, roledId {}, memberId {}, assignment {}", savedMembership.getRoleId(), savedMembership.getMemberId(), assignment.getId(), e);
            }
        });
    }
}
