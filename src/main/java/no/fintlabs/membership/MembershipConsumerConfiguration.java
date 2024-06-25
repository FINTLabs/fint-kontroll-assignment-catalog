package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.kafka.entity.EntityConsumerFactoryService;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

@Slf4j
@Configuration
public class MembershipConsumerConfiguration {

    private final MembershipRepository membershipRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssignmentService assignmentService;

    public MembershipConsumerConfiguration(MembershipRepository membershipRepository, AssignmentService assignmentService, FlattenedAssignmentService flattenedAssignmentService) {
        this.membershipRepository = membershipRepository;
        this.assignmentService = assignmentService;
        this.flattenedAssignmentService = flattenedAssignmentService;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, Membership> membershipConsumer(
            EntityConsumerFactoryService entityConsumerFactoryService
    ) {
        return entityConsumerFactoryService.createFactory(
                        Membership.class,
                        this::processMemberships)
                .createContainer(EntityTopicNameParameters
                                         .builder()
                                         .resource("role-catalog-membership")
                                         .build());
    }

    private void processMemberships(ConsumerRecord<String, Membership> consumerRecord) {
        Membership incomingMembership = consumerRecord.value();

        membershipRepository.findById(incomingMembership.getId())
                .ifPresentOrElse(
                        existingMembership -> processExistingMembership(existingMembership, incomingMembership),
                        () -> processNewMembership(incomingMembership));
    }

    private void processNewMembership(Membership incomingMembership) {
        log.info("Incoming membership does not exist. Saving it, roleId {}, memberId {}", incomingMembership.getRoleId(), incomingMembership.getMemberId());
        Membership savedMembership = membershipRepository.save(incomingMembership);

        processAssignmentsForMembership(savedMembership);
    }

    private void processExistingMembership(Membership existingMembership, Membership incomingMembership) {
        if (!existingMembership.equals(incomingMembership)) {
            log.info("Membership already exist but is different from incoming. Saving it, roleId {}, memberId {}", incomingMembership.getRoleId(), incomingMembership.getMemberId());
            membershipRepository.save(incomingMembership);
        } else {
            log.info("Membership already exist and is equal to incoming. Skipping, roleId {}, memberId {}", incomingMembership.getRoleId(), incomingMembership.getMemberId());
        }

        processAssignmentsForMembership(existingMembership);
    }

    @Async("processAssignmentsForMembership")
    void processAssignmentsForMembership(Membership savedMembership) {
        List<Assignment> allAssignments = assignmentService.getAssignmentsByRole(savedMembership.getRoleId());
        allAssignments.forEach(assignment -> {
            try {
                flattenedAssignmentService.createFlattenedAssignments(assignment, false);
            } catch (Exception e) {
                log.error("Error processing assignments for membership, roledId {}, memberId {}, assignment {}", savedMembership.getRoleId(), savedMembership.getMemberId(), assignment.getId(), e);
            }
        });
    }
}
