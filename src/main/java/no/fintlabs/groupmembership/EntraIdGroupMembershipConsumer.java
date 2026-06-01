package no.fintlabs.groupmembership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.common.KafkaConsumerConfigurationDefaults;
import no.fintlabs.kafka.KafkaEventTopics;
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.Async;

import java.util.*;

@Slf4j
@Configuration
public class EntraIdGroupMembershipConsumer {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;
    private final KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults;


    public EntraIdGroupMembershipConsumer(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                          AssigmentEntityProducerService assigmentEntityProducerService,
                                          FlattenedAssignmentService flattenedAssignmentService,
                                          KafkaConsumerConfigurationDefaults kafkaConsumerConfigurationDefaults) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.kafkaConsumerConfigurationDefaults = kafkaConsumerConfigurationDefaults;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, EntraIdGroupMembership> entraIdMembershipConsumer(
            ParameterizedListenerContainerFactoryService entityConsumerFactoryService
    ) {

        return entityConsumerFactoryService.createRecordListenerContainerFactory(
                        EntraIdGroupMembership.class,
                        this::processGroupMembership,
                        KafkaEventTopics.defaultListenerConfiguration(),
                        kafkaConsumerConfigurationDefaults.defaultErrorHandler())
                .createContainer(KafkaEventTopics.topicNameParameters("graph-user-group-membership"));

    }

    @Async
    void processGroupMembership(ConsumerRecord<String, EntraIdGroupMembership> record) {
        EntraIdGroupMembership membership = record.value();

        if (membership == null) {
            log.warn("Skipping null Entra membership result for key {}", record.key());
            return;
        }

        if (membership.getCode() == null) {
            log.warn("Skipping Entra membership result with missing code for key {}", record.key());
            return;
        }

        switch (membership.getCode()) {
            case ADDED -> handleUpdate(membership);
            case REMOVED -> handleDeletion(membership);
            case NO_CHANGES -> handleNoChanges(membership);
            case ERROR, FAILED -> log.warn("Entra membership operation for groupref {} - userref {} completed with status {}",
                    membership.getEntraGroupRef(),
                    membership.getEntraUserRef(),
                    membership.getCode());
            default -> log.warn("Unhandled Entra membership status {} for key {}", membership.getCode(), record.key());
        }
    }

    private void handleNoChanges(EntraIdGroupMembership membership) {
        log.info("Handling no-changes result with groupref {} - userref {}", membership.getEntraGroupRef(), membership.getEntraUserRef());

        try {
            UUID groupId = membership.getEntraGroupRef();
            UUID userId = membership.getEntraUserRef();

            List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);

            List<FlattenedAssignment> activeAssignmentsToConfirm = flattenedAssignments.stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipConfirmed())
                    .peek(assignment -> {
                        log.info("Membership already exists in Entra. Marking flattenedassignmentId {} as confirmed", assignment.getId());
                        assignment.setIdentityProviderGroupMembershipConfirmed(true);
                    })
                    .toList();

            List<FlattenedAssignment> deletedAssignmentsToConfirm = flattenedAssignments.stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .peek(assignment -> {
                        log.info("Membership already removed in Entra. Marking flattenedassignmentId {} deletion as confirmed", assignment.getId());
                        assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                    })
                    .toList();

            List<FlattenedAssignment> assignmentsToSave = new ArrayList<>();
            assignmentsToSave.addAll(activeAssignmentsToConfirm);
            assignmentsToSave.addAll(deletedAssignmentsToConfirm);

            if (!assignmentsToSave.isEmpty()) {
                flattenedAssignmentService.saveFlattenedAssignmentsBatch(assignmentsToSave);
            }
        } catch (Exception e) {
            log.error("Failed to handle no-changes result for groupref {} - userref {}. Error: {}",
                    membership.getEntraGroupRef(),
                    membership.getEntraUserRef(),
                    e.getMessage());
        }
    }

    private void handleDeletion(EntraIdGroupMembership membership) {
        log.info("Handling deletion result with groupref {} - userref {}", membership.getEntraGroupRef(), membership.getEntraUserRef());

        try {
            UUID groupId = membership.getEntraGroupRef();
            UUID userId = membership.getEntraUserRef();

            List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);
            FlattenedAssignment newestAssignment = getNewest(flattenedAssignments).orElse(null);
            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .forEach(assignment -> {
                        log.info("Found assignment for deletion. flattenedassignmentId: {}", assignment.getId());
                        assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                        flattenedAssignmentRepository.saveAndFlush(assignment);
                    });

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                    .forEach(assignment -> {
                        log.info("Found inconsistent assignment on deletion, identityProviderGroupMembershipDeletionConfirmed is false. FlattenedAssignmentId: {}", assignment.getId());
                        if (newestAssignment != null && assignment.getId().equals(newestAssignment.getId())) {
                            log.info("FlattenedAssignment with id: {} is the most recent, publishing", assignment.getId());
                            assigmentEntityProducerService.publish(assignment);
                        }
                    });

            flattenedAssignments
                    .stream()
                    .filter(assignment -> assignment.getAssignmentTerminationDate() == null && assignment.isIdentityProviderGroupMembershipConfirmed())
                    .forEach(assignment -> {
                        log.info("Found inconsistent delete, identityProviderGroupMembershipConfirmed is true. FlattenedAssignmentId: {}", assignment.getId());
                        if (newestAssignment != null && assignment.getId().equals(newestAssignment.getId())) {
                            log.info("FlattenedAssignment with id: {} is the most recent, publishing", assignment.getId());
                            assigmentEntityProducerService.publish(assignment);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to handle deletion for groupref {} - userref {}. Error: {}",
                    membership.getEntraGroupRef(),
                    membership.getEntraUserRef(),
                    e.getMessage());
        }
    }

    private Optional<FlattenedAssignment> getNewest(List<FlattenedAssignment> flattenedAssignments) {
        return flattenedAssignments.stream()
                .max(Comparator.comparing(FlattenedAssignment::getId));
    }


    private void handleUpdate(EntraIdGroupMembership membership) {
        log.debug("Received update with groupref {} - userref {}", membership.getEntraGroupRef(), membership.getEntraUserRef());

        try {
            UUID groupId = membership.getEntraGroupRef();
            UUID userId = membership.getEntraUserRef();

            List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(groupId, userId);

            if (flattenedAssignments.isEmpty()) {
                assigmentEntityProducerService.publishDeletion(groupId, userId);
                log.info("User assignment not found with id: {}, removing user from group: {} in Entra ID", userId, groupId);
            } else {
                handleValidMembershipUpdate(membership, flattenedAssignments);
            }

        } catch (Exception e) {
            log.error("Failed to handle update for groupref {} - userref {}. Error: {}", membership.getEntraGroupRef(), membership.getEntraUserRef(), e.getMessage());
        }
    }

    private void handleValidMembershipUpdate(EntraIdGroupMembership membership, List<FlattenedAssignment> flattenedAssignments) {
        List<FlattenedAssignment> assignmentsToUpdate = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() == null && !assignment.isIdentityProviderGroupMembershipConfirmed())
                .peek(assignment -> {
                    log.info("Received update with groupref {} - userref {}, saving as confirmed on flattenedassignmentId: {}", membership.getEntraGroupRef(), membership.getEntraUserRef(),
                            assignment.getId());
                    assignment.setIdentityProviderGroupMembershipConfirmed(true);
                })
                .toList();
        FlattenedAssignment newestAssignment = getNewest(flattenedAssignments).orElse(null);

        List<FlattenedAssignment> assignmentsToDelete = flattenedAssignments.stream()
                .filter(assignment -> assignment.getAssignmentTerminationDate() != null && !assignment.isIdentityProviderGroupMembershipDeletionConfirmed())
                .peek(assignment -> {
                    log.info("Found inconsistent assignment on update, updating and publishing. flattenedassignmentId: {}", assignment.getId());
                })
                .toList();

        if (!assignmentsToUpdate.isEmpty()) {
            flattenedAssignmentService.saveFlattenedAssignmentsBatch(assignmentsToUpdate);
        }

        if (!assignmentsToDelete.isEmpty()) {
            List<FlattenedAssignment> toSave = new ArrayList<>();

            assignmentsToDelete.forEach(assignment -> {
                if (newestAssignment != null && assignment.getId().equals(newestAssignment.getId())) {
                    log.info("FlattenedAssignment with id: {} is the most recent, publishing", assignment.getId());
                    assigmentEntityProducerService.publishDeletion(assignment);
                } else {
                    assignment.setIdentityProviderGroupMembershipDeletionConfirmed(true);
                    toSave.add(assignment);
                }
            });

            if (!toSave.isEmpty()) {
                flattenedAssignmentService.saveFlattenedAssignmentsBatch(toSave);
            }
        }
    }
}
