package no.fintlabs.assignment.flattened;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlattenedAssignmentRepository extends JpaRepository<FlattenedAssignment, Long>, JpaSpecificationExecutor<FlattenedAssignment> {
    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();

    List<FlattenedAssignment> findByAssignmentId(Long assignmentId);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentIdAndAssignmentTerminationDateIsNull(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, Long assignmentId);
    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, Long assignmentId);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId);

    Optional<FlattenedAssignment> findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(Long userRef, Long resourceRef);

    List<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(
            UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean groupMembershipDeletionConfirmed);
}
