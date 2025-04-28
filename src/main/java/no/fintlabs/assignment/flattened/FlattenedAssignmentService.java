package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;

@Slf4j
@Service
public class FlattenedAssignmentService {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    private final FlattenedAssignmentMapper flattenedAssignmentMapper;

    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public FlattenedAssignmentService(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                      FlattenedAssignmentMapper flattenedAssignmentMapper,
                                      FlattenedAssignmentMembershipService flattenedAssignmentMembershipService,
                                      AssigmentEntityProducerService assigmentEntityProducerService) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.flattenedAssignmentMembershipService = flattenedAssignmentMembershipService;
        this.flattenedAssignmentMapper = flattenedAssignmentMapper;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    @Async
    @Transactional
    public void createFlattenedAssignments(Assignment assignment) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        log.info("Creating flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        if (assignment.isUserAssignment()) {
            flattenedAssignments.add(toFlattenedAssignment(assignment));
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.createFlattenedAssignmentsForNewRoleAssignment(assignment));
        } else {
            log.error("Assignment with id {} is not a user or group assignment, not creating flattened assignment", assignment.getId());
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, false);
        }
    }

    @Async
    @Transactional
    public void syncFlattenedAssignments(Assignment assignment, boolean isSync) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot update flattened assignment");
            return;
        }

        log.info("Updating flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        List<FlattenedAssignment> existingAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());

        if (assignment.isUserAssignment()) {
            FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments, isSync)
                    .ifPresent(flattenedAssignments::add);
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, isSync));
        } else {
            log.error("Assignment with id {} is not a user or group assignment, not updating any flattened assignment", assignment.getId());
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, isSync);
        }
    }

    @Transactional
    public void createFlattenedAssignmentsForMembership(Assignment assignment, Membership membership) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        Long userRef = membership.getMemberId();
        Long roleRef = membership.getRoleId();

        flattenedAssignmentRepository.findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(assignment.getId(), userRef, roleRef)
                .ifPresentOrElse(flattenedAssignment -> {
                                     //TODO: sjekk på status endring
                                     if (membership.getIdentityProviderUserObjectId() != null && !membership.getIdentityProviderUserObjectId().equals(flattenedAssignment.getIdentityProviderUserObjectId())) {
                                         log.info("Found flattened assignment for role {}, user {} and assignment {}. Updating it", roleRef, userRef, assignment.getId());
                                         flattenedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());

                                         flattenedAssignments.add(flattenedAssignment);
                                     }
                                 }, () -> {
                                     log.info("No flattened assignment found for role {}, user {} and assignment {}. Creating new", roleRef, userRef, assignment.getId());
                                     FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
                                     mappedAssignment.setUserRef(userRef);
                                     mappedAssignment.setAssignmentViaRoleRef(roleRef);
                                     mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
                                     flattenedAssignments.add(mappedAssignment);
                                 }

                );

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, false);
        }
    }

    public void saveFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate) {
        log.info("saveFlattened - Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            List<FlattenedAssignment> savedFlattened = flattenedAssignmentRepository.saveAll(batch);

            savedFlattened.forEach(flattenedAssignment -> {
                log.info("saveFlattened - Saved flattened assignment with id: {}, assignmentId: {}", flattenedAssignment.getId(), flattenedAssignment.getAssignmentId());
            });
        }

        flattenedAssignmentRepository.flush();

        log.info("saveFlattened - Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    public void saveAndPublishFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate, boolean isSync) {
        log.info("saveAndPublish - {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            List<FlattenedAssignment> savedFlattened = flattenedAssignmentRepository.saveAll(batch);

            savedFlattened.forEach(flattenedAssignment -> {
                log.info("saveAndPublish - Flattened assignment with id: {}, assignmentId: {}", flattenedAssignment.getId(), flattenedAssignment.getAssignmentId());
            });

            if (!isSync) {
                log.info("saveAndPublish - Publishing {} new flattened assignments to azure", batch.size());
                batch.forEach(assigmentEntityProducerService::publish);
            }
        }

        flattenedAssignmentRepository.flush();

        log.info("saveAndPublish - Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    @Transactional
    public void deleteFlattenedAssignments(Assignment assignment) {
        log.info("Deactivate flattened assignments for assignment with id {}", assignment.getId());

        String deactivationReason = "Assosiated assignment removed by user";
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());
        deactivateFlattenedAssignments(flattenedAssignments, deactivationReason, assignment.getAssignmentRemovedDate());
    }

    @Transactional
    public void deactivateFlattenedAssignments(Set<Long> flattenedAssignmentIds) {
        log.info("Deactivate flattened assignments:  {}", flattenedAssignmentIds);

        String deactivationReason = "Role membership deactivated";
        Date deactivationDate = new Date();
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentIds
                .stream()
                .map(flattenedAssignmentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        deactivateFlattenedAssignments(flattenedAssignments, deactivationReason, deactivationDate);
    }

    public List<FlattenedAssignment> getAllFlattenedAssignments() {
        return flattenedAssignmentRepository.findAll();
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed() {
        return flattenedAssignmentRepository.findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(false);
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmedByAssignmentId(Long assignmentId) {
        return flattenedAssignmentRepository.findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNullAndAssignmentId(false, assignmentId);
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsDeletedNotConfirmed() {
        return flattenedAssignmentRepository.findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsDeletedNotConfirmedByAssignmentId(Long assignmentId) {
        return flattenedAssignmentRepository.findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalseAndAssignmentId(assignmentId);
    }

    public Optional<FlattenedAssignment> getFlattenedAssignmentByUserAndResourceNotTerminated(Long userRef, Long resourceRef) {
        return flattenedAssignmentRepository.findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(userRef, resourceRef);
    }

    public Set<Long> findFlattenedAssignmentIdsByUserAndRoleRef(Long userRef, Long roleRef) {
        return new HashSet<>(flattenedAssignmentRepository.findFlattenedAssignmentIdsByUserAndRoleRef(userRef, roleRef));
    }

    public Set<Long> getIdsMissingIdentityProviderUserObjectId() {
        return new HashSet<>(flattenedAssignmentRepository.findIdsWhereIdentityProviderUserObjectIdIsNull());
    }

    private void deactivateFlattenedAssignments(List<FlattenedAssignment> flattenedAssignments, String deactivationReason, Date deactivationDate) {
        if (flattenedAssignments.isEmpty()) {
            log.info("No flattened assignments found for deactivation");
            return;
        }
        flattenedAssignments.forEach(flattenedAssignment -> {
            log.info("Deactivate flattened assignment for with id: {} for assignment id {}, deactivationReason: {}",
                     flattenedAssignment.getId(),
                     flattenedAssignment.getAssignmentId(),
                     deactivationReason
            );
            flattenedAssignment.setAssignmentTerminationReason(deactivationReason);
            flattenedAssignment.setAssignmentTerminationDate(deactivationDate);
            flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);
            assigmentEntityProducerService.publishDeletion(flattenedAssignment);
        });
    }
}
