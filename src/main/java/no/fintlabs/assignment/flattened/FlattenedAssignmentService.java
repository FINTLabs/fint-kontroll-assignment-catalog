package no.fintlabs.assignment.flattened;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.stereotype.Service;

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
        if (assignment.getUserRef() != null) {
            saveFlattenedAssignment(assignment);
        } else if (assignment.getRoleRef() != null) {
            List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

            if (memberships.isEmpty()) {
                log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
            } else {
                log.info("Saving flattened assignments for roleref {}", assignment.getRoleRef());
                memberships.forEach(membership -> {
                    assignment.setAzureAdUserId(membership.getIdentityProviderUserObjectId());
                    assignment.setUserRef(membership.getMemberId());
                    saveFlattenedAssignment(assignment);
                });
            }
        }
    }

    private void saveFlattenedAssignment(Assignment assignment) {
        flattenedAssignmentRepository.findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNull(assignment.getAzureAdGroupId(),
                                                                                                         assignment.getAzureAdUserId())
                .ifPresentOrElse(
                        flattenedAssignment -> {
                            log.info("Flattened assignment already exists. Updating assignment with id {}", assignment.getId());
                            FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
                            mappedAssignment.setId(flattenedAssignment.getId());
                            mappedAssignment.setIdentityProviderGroupMembershipDeletionConfirmed(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
                            mappedAssignment.setIdentityProviderGroupMembershipConfirmed(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
                            flattenedAssignmentRepository.save(mappedAssignment);
                        },
                        () -> {
                            log.info("Flattened assignment does not exist. Saving assignment with id {}", assignment.getId());
                            flattenedAssignmentRepository.save(toFlattenedAssignment(assignment));
                        }
                );
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
