package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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

    public FlattenedAssignmentService(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                      FlattenedAssignmentMapper flattenedAssignmentMapper,
                                      FlattenedAssignmentMembershipService flattenedAssignmentMembershipService) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.flattenedAssignmentMembershipService = flattenedAssignmentMembershipService;
        this.flattenedAssignmentMapper = flattenedAssignmentMapper;
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
            flattenedAssignments.add(flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments, isSync));
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.findMembershipsToCreateOrUpdate(assignment, existingAssignments, isSync));
        }

        if (!flattenedAssignments.isEmpty()) {
            saveFlattenedAssignments(flattenedAssignments);
        }
    }

    private void saveFlattenedAssignments(List<FlattenedAssignment> flattenedAssignmentsForUpdate) {
        log.info("Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 100; // Or any optimal batch size
        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            flattenedAssignmentRepository.saveAll(batch);
            flattenedAssignmentRepository.flush();
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
