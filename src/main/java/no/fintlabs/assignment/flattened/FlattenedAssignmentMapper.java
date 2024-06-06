package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class FlattenedAssignmentMapper {


    public Optional<FlattenedAssignment> mapOriginWithExisting(FlattenedAssignment originalAssignment, List<FlattenedAssignment> existingAssignments, boolean isSync) {
        long start = System.currentTimeMillis();

        for (FlattenedAssignment existingAssignment : existingAssignments) {
            if (existingAssignment.getIdentityProviderGroupObjectId() != null
                && existingAssignment.getIdentityProviderUserObjectId() != null
                && existingAssignment.getIdentityProviderGroupObjectId().equals(originalAssignment.getIdentityProviderGroupObjectId())
                && existingAssignment.getIdentityProviderUserObjectId().equals(originalAssignment.getIdentityProviderUserObjectId())) {

                if (isSync) {
                    if (hasNoChanges(originalAssignment, existingAssignment)) {
                        return Optional.empty();
                    }

                    log.info(
                            "Flattened assignment already exist. Updating flattenedassignment with id: {}, assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, " +
                            "azureadgroupid:" +
                            " {}",
                            originalAssignment.getId(), existingAssignment.getAssignmentId(), originalAssignment.getUserRef(), originalAssignment.getAssignmentViaRoleRef(),
                            originalAssignment.getIdentityProviderUserObjectId(), originalAssignment.getIdentityProviderGroupObjectId());

                    mapWithExisting(originalAssignment, existingAssignment);
                    return Optional.of(originalAssignment);
                } else {
                    if (existingAssignment.getAssignmentTerminationDate() == null) {
                        mapWithExisting(originalAssignment, existingAssignment);
                        return Optional.of(originalAssignment);
                    }

                    return Optional.empty();
                }
            } else {
                log.error("Existing assignment does not have identityProviderGroupObjectId {} or identityProviderUserObjectId {}, flattenedassignmentId: {}. Cannot map assignment",
                          originalAssignment.getIdentityProviderGroupObjectId(), originalAssignment.getIdentityProviderUserObjectId(), existingAssignment.getId());
                return Optional.empty();
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("Time taken to map with existing flattened assignment: " + (endTime - start) + " ms");

        return Optional.of(originalAssignment);
    }

    private boolean hasNoChanges(FlattenedAssignment originalAssignment, FlattenedAssignment existingAssignment) {
        return Objects.equals(originalAssignment.getAssignmentId(), existingAssignment.getAssignmentId()) &&
               Objects.equals(originalAssignment.getUserRef(), existingAssignment.getUserRef()) &&
               Objects.equals(originalAssignment.getAssignmentViaRoleRef(), existingAssignment.getAssignmentViaRoleRef()) &&
               Objects.equals(originalAssignment.getResourceRef(), existingAssignment.getResourceRef()) &&
               Objects.equals(originalAssignment.getAssignmentTerminationDate(), existingAssignment.getAssignmentTerminationDate()) &&
               Objects.equals(originalAssignment.getIdentityProviderUserObjectId(), existingAssignment.getIdentityProviderUserObjectId()) &&
               Objects.equals(originalAssignment.getIdentityProviderGroupObjectId(), existingAssignment.getIdentityProviderGroupObjectId());
    }

    private void mapWithExisting(FlattenedAssignment origin, FlattenedAssignment existing) {
        origin.setId(existing.getId());
        origin.setIdentityProviderGroupMembershipDeletionConfirmed(existing.isIdentityProviderGroupMembershipDeletionConfirmed());
        origin.setIdentityProviderGroupMembershipConfirmed(existing.isIdentityProviderGroupMembershipConfirmed());
    }
}
