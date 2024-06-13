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
        if (existingAssignments.isEmpty()) {
            return Optional.of(originalAssignment);
        }

        for (FlattenedAssignment existingAssignment : existingAssignments) {
            //assignment finnes med samme assignmentid
            if (Objects.equals(originalAssignment.getIdentityProviderUserObjectId(), existingAssignment.getIdentityProviderUserObjectId()) &&
                Objects.equals(originalAssignment.getIdentityProviderGroupObjectId(), existingAssignment.getIdentityProviderGroupObjectId())) {

                //assignment finnes med samme identityProviderUserObjectId og identityProviderGroupObjectId
                if (isSync) {
                    // hvis sync, oppdater hvis det finnes endringer
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
                    // hvis post, oppdater hvis ikke sagt opp
                    if (existingAssignment.getAssignmentTerminationDate() == null) {
                        mapWithExisting(originalAssignment, existingAssignment);
                        return Optional.of(originalAssignment);
                    }

                    return Optional.empty();
                }
            }

            // kjør videre, sjekk neste flattened
        }

        // hvis kommet hit finnes det flattened assignments med samme id, men ikke med samme identityProviderUserObjectId og identityProviderGroupObjectId, droppe?
        log.info(
                "Flattened assignment already exist. Skipping flattenedassignment with id: {}, assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, " +
                "azureadgroupid: {}",
                originalAssignment.getId(), originalAssignment.getAssignmentId(), originalAssignment.getUserRef(), originalAssignment.getAssignmentViaRoleRef(),
                originalAssignment.getIdentityProviderUserObjectId(), originalAssignment.getIdentityProviderGroupObjectId());

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

    private void mapWithExisting(FlattenedAssignment origin, FlattenedAssignment existing) {
        origin.setId(existing.getId());
        origin.setIdentityProviderGroupMembershipDeletionConfirmed(existing.isIdentityProviderGroupMembershipDeletionConfirmed());
        origin.setIdentityProviderGroupMembershipConfirmed(existing.isIdentityProviderGroupMembershipConfirmed());
    }
}
