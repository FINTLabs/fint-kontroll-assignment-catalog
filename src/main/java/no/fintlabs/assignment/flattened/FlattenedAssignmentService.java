package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public void createFlattenedAssignments(Assignment assignment, boolean isSync) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        log.info("Creating flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        List<FlattenedAssignment> existingAssignments = flattenedAssignmentRepository.findByAssignmentId(assignment.getId());

        if (assignment.isUserAssignment()) {
            FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments, isSync)
                    .ifPresent(flattenedAssignments::add);
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.findMembershipsToCreateOrUpdate(assignment, existingAssignments, isSync));
        }

        if (!flattenedAssignments.isEmpty()) {
            saveFlattenedAssignmentsBatch(flattenedAssignments, isSync);
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
                                     if (!flattenedAssignment.getIdentityProviderUserObjectId().equals(membership.getIdentityProviderUserObjectId())) {
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
            saveFlattenedAssignmentsBatch(flattenedAssignments, false);
        }
    }

    public void saveFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate, boolean isSync) {
        log.info("Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            flattenedAssignmentRepository.saveAll(batch);

            if (!isSync) {
                log.info("Publishing {} new flattened assignments to azure", batch.size());
                batch.forEach(assigmentEntityProducerService::publish);
            }
        }

        flattenedAssignmentRepository.flush();

        log.info("Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    @Transactional
    public void deleteFlattenedAssignments(Assignment assignment) {
        log.info("Deleting flattened assignments for assignment with id {}", assignment.getId());

        flattenedAssignmentRepository.findByAssignmentId(assignment.getId())
                .forEach(flattenedAssignment -> {
                    log.info("Deleting flattened assignment for with id: {}, for assignment id {}", flattenedAssignment.getId(), flattenedAssignment.getAssignmentId());
                    flattenedAssignment.setAssignmentTerminationDate(assignment.getAssignmentRemovedDate());
                    flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);
                    assigmentEntityProducerService.publishDeletion(flattenedAssignment);

                });
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
}
