package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
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
            saveFlattenedAssignments(flattenedAssignments, isSync);
        }
    }

    @Transactional
    public void createFlattenedAssignmentsForMembership(Assignment assignment, Long userRef, Long roleRef) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        log.info("Creating flattened assignments for assignment with id {} found with roleRef: {}, userRef: {}", assignment.getId(), roleRef, userRef);
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();
        FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);

        flattenedAssignmentRepository.findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(assignment.getId(), userRef, roleRef)
                .ifPresentOrElse(flattenedAssignment -> {
                                     log.info("Found flattened assignment for role {}, user {} and assignment {}. Updating it", roleRef, userRef, assignment.getId());
                                     flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, List.of(flattenedAssignment), false)
                                             .ifPresent(flattenedAssignments::add);
                                 }, () -> {
                                     log.info("No flattened assignment found for role {}, user {} and assignment {}. Creating new", roleRef, userRef, assignment.getId());
                                     mappedAssignment.setUserRef(userRef);
                                     mappedAssignment.setAssignmentViaRoleRef(roleRef);
                                     flattenedAssignments.add(mappedAssignment);
                                 }

                );

        if (!flattenedAssignments.isEmpty()) {
            saveFlattenedAssignments(flattenedAssignments, false);
        }
    }

    private void saveFlattenedAssignments(List<FlattenedAssignment> flattenedAssignmentsForUpdate, boolean isSync) {
        log.info("Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            flattenedAssignmentRepository.saveAll(batch);
            flattenedAssignmentRepository.flush();

            if (!isSync) {
                log.info("Publishing {} new flattened assignments to azure", batch.size());
                batch.forEach(assigmentEntityProducerService::publish);
            }
        }
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
                });
    }

    public List<FlattenedAssignment> getAllFlattenedAssignments() {
        return flattenedAssignmentRepository.findAll();
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed() {
        return flattenedAssignmentRepository.findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(false);
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsDeletedNotConfirmed() {
        return flattenedAssignmentRepository.findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();
    }

    public Optional<FlattenedAssignment> getFlattenedAssignmentByUserAndResourceNotTerminated(Long userRef, Long resourceRef) {
        return flattenedAssignmentRepository.findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(userRef, resourceRef);
    }
}
