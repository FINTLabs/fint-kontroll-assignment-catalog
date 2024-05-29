package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;

@Slf4j
@Service
public class FlattenedAssignmentService {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    private final MembershipRepository membershipRepository;

    public FlattenedAssignmentService(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                      MembershipRepository membershipRepository) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public void createFlattenedAssignments(Assignment assignment) {
        log.info("Creating flattened assignments for assignment with id {}", assignment.getId());
        createOrUpdateFlattenedAssignment(assignment);
    }

    @Transactional
    public void updateFlattenedAssignment(Assignment assignment) {
        log.info("Updating flattened assignment with id {}", assignment.getId());
        createOrUpdateFlattenedAssignment(assignment);
    }

    private void createOrUpdateFlattenedAssignment(Assignment assignment) {
        if(assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        List<FlattenedAssignment> flattenedAssignmentsForUpdate = new ArrayList<>();
        FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);

        if (assignment.getUserRef() != null) {
            flattenedAssignmentsForUpdate.add(mapForUpdateOrCreate(mappedAssignment));
        } else if (assignment.getRoleRef() != null) {
            List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

            if (memberships.isEmpty()) {
                log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
            } else {
                log.info("Preparing all memberships to save as flattened assignments for roleref {}", assignment.getRoleRef());

                memberships.forEach(membership -> {
                    mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
                    mappedAssignment.setUserRef(membership.getMemberId());
                    flattenedAssignmentsForUpdate.add(mapForUpdateOrCreate(mappedAssignment));
                });

                log.info("Added: {} memberships/groups to save", flattenedAssignmentsForUpdate.size());
            }
        }

        if (!flattenedAssignmentsForUpdate.isEmpty()) {
            log.info("Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
            flattenedAssignmentRepository.saveAllAndFlush(flattenedAssignmentsForUpdate);
        }
    }

    private FlattenedAssignment mapForUpdateOrCreate(FlattenedAssignment flattenedAssignment) {
        log.info("Finding flattened assignment by azureadgroupid: {} and azureaduserid: {}",
                 flattenedAssignment.getIdentityProviderGroupObjectId(),
                 flattenedAssignment.getIdentityProviderUserObjectId());

        flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNull(flattenedAssignment.getIdentityProviderGroupObjectId(),
                                                                                                                                           flattenedAssignment.getIdentityProviderUserObjectId())
                .ifPresentOrElse(
                        foundFlattenedAssignment -> {
                            log.info("Flattened assignment already exist. Updating flattenedassignment with assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, azureadgroupid: {}",
                                     flattenedAssignment.getId(), flattenedAssignment.getUserRef(), flattenedAssignment.getAssignmentViaRoleRef(),
                                     flattenedAssignment.getIdentityProviderUserObjectId(), flattenedAssignment.getIdentityProviderGroupObjectId());

                            foundFlattenedAssignment.setId(foundFlattenedAssignment.getId());
                            foundFlattenedAssignment.setIdentityProviderGroupMembershipDeletionConfirmed(foundFlattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
                            foundFlattenedAssignment.setIdentityProviderGroupMembershipConfirmed(foundFlattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
                        },
                        () -> log.info("Flattened assignment does not exist. Creating new with assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, azureadgroupid: {}",
                                       flattenedAssignment.getId(), flattenedAssignment.getUserRef(), flattenedAssignment.getAssignmentViaRoleRef(),
                                       flattenedAssignment.getIdentityProviderUserObjectId(), flattenedAssignment.getIdentityProviderGroupObjectId())
                );

        return flattenedAssignment;
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
