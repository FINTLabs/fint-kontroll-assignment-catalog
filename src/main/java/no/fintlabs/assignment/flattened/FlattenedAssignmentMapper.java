package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class FlattenedAssignmentMapper {

    public Optional<FlattenedAssignment> mapOriginWithExisting(FlattenedAssignment originalAssignment, List<FlattenedAssignment> existingAssignments) {
        log.info("Trying to map flattened assignment with assignmentId: {}, userref: {}, via roleref: {} and resourceref: {} to existing assignments",
                 originalAssignment.getAssignmentId(), originalAssignment.getUserRef(), originalAssignment.getAssignmentViaRoleRef(), originalAssignment.getResourceRef());

       for (FlattenedAssignment existingAssignment : existingAssignments) {
            if (Objects.equals(originalAssignment.getIdentityProviderUserObjectId(), existingAssignment.getIdentityProviderUserObjectId()) &&
                Objects.equals(originalAssignment.getIdentityProviderGroupObjectId(), existingAssignment.getIdentityProviderGroupObjectId())
            ) {
                if (hasNoChanges(originalAssignment, existingAssignment)) {
                    return Optional.empty(); // No changes, skip
                }

                log.info(
                        "Flattened assignment already exist. Updating flattened assignment with  assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, " +
                        "azureadgroupid:" +
                        " {}",
                        existingAssignment.getAssignmentId(), originalAssignment.getUserRef(), originalAssignment.getAssignmentViaRoleRef(),
                        originalAssignment.getIdentityProviderUserObjectId(), originalAssignment.getIdentityProviderGroupObjectId());

                mapWithOrigin(originalAssignment, existingAssignment);
                return Optional.of(existingAssignment);
            }
        }
        if (originalAssignment.getAssignmentTerminationDate() == null) {
            log.info("No existing flattened assignment match. Should create new,  assignmentId: {}, userref: {}, roleref: {}",
                    originalAssignment.getAssignmentId(), originalAssignment.getUserRef(), originalAssignment.getAssignmentViaRoleRef());

            return Optional.of(originalAssignment);
        }
        return Optional.empty();
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

    private void mapWithOrigin(FlattenedAssignment origin, FlattenedAssignment existing) {
        if (existing.getAssignmentTerminationDate() == null) {
            existing.setAssignmentTerminationDate(origin.getAssignmentTerminationDate());
        }
    }
}
