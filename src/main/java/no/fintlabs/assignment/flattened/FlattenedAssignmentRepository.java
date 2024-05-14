package no.fintlabs.assignment.flattened;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlattenedAssignmentRepository extends JpaRepository<FlattenedAssignment, Long>, JpaSpecificationExecutor<FlattenedAssignment> {
    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndIdentityProviderGroupMembershipConfirmedAndAssignmentTerminationDateIsNull(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByIdentityProviderGroupMembershipConfirmed(boolean identityProviderGroupMembershipConfirmed);

    List<FlattenedAssignment> findByAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmedFalse();

    Optional<FlattenedAssignment> findByAssignmentId(Long assignmentId);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNull(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectId(UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId);

    Optional<FlattenedAssignment> findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(Long userRef, Long resourceRef);

    Optional<FlattenedAssignment> findByIdentityProviderGroupObjectIdAndIdentityProviderUserObjectIdAndAssignmentTerminationDateIsNotNullAndIdentityProviderGroupMembershipDeletionConfirmed(
            UUID identityProviderGroupObjectId, UUID identityProviderUserObjectId, boolean groupMembershipDeletionConfirmed);
}
