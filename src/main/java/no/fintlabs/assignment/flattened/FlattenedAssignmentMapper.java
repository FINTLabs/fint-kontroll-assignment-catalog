package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlattenedAssignmentMapper {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    public FlattenedAssignmentMapper(FlattenedAssignmentRepository flattenedAssignmentRepository) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
    }

    public FlattenedAssignment mapOriginWithExisting(FlattenedAssignment flattenedAssignment, boolean isSync) {
        log.info("Finding flattened assignment by azureadgroupid: {}, azureaduserid: {} and assignmentId: {}",
                 flattenedAssignment.getIdentityProviderGroupObjectId(),
                 flattenedAssignment.getIdentityProviderUserObjectId(),
                 flattenedAssignment.getAssignmentId());

        long start = System.currentTimeMillis();

        flattenedAssignmentRepository.findByAssignmentId(flattenedAssignment.getAssignmentId())
                .stream().filter(
                        foundFlattenedAssignment -> foundFlattenedAssignment.getIdentityProviderGroupObjectId().equals(flattenedAssignment.getIdentityProviderGroupObjectId())
                                                    && foundFlattenedAssignment.getIdentityProviderUserObjectId().equals(flattenedAssignment.getIdentityProviderUserObjectId()))
                .forEach(
                        foundFlattenedAssignment -> {
                            long startMap = System.currentTimeMillis();
                            if (isSync) {
                                log.info(
                                        "Flattened assignment already exist. Updating flattenedassignment with id: {}, assignmentId: {}, userref: {}, roleref: {}, azureaduserid: {}, azureadgroupid:" +
                                        " {}",
                                        flattenedAssignment.getId(), foundFlattenedAssignment.getAssignmentId(), flattenedAssignment.getUserRef(), flattenedAssignment.getAssignmentViaRoleRef(),
                                        flattenedAssignment.getIdentityProviderUserObjectId(), flattenedAssignment.getIdentityProviderGroupObjectId());

                                mapWithExisting(flattenedAssignment, foundFlattenedAssignment);
                            } else {
                                if (foundFlattenedAssignment.getAssignmentTerminationDate() == null) {
                                    mapWithExisting(flattenedAssignment, foundFlattenedAssignment);
                                }
                            }
                            long endMap = System.currentTimeMillis();
                            log.info("Time taken to map flattened assignment: " + (endMap - startMap) + " ms");
                        }

                );

        long endTime = System.currentTimeMillis();
        log.info("Time taken to find flattened assignment: " + (endTime - start) + " ms");

        return flattenedAssignment;
    }

    private void mapWithExisting(FlattenedAssignment origin, FlattenedAssignment existing) {
        origin.setId(existing.getId());
        origin.setIdentityProviderGroupMembershipDeletionConfirmed(existing.isIdentityProviderGroupMembershipDeletionConfirmed());
        origin.setIdentityProviderGroupMembershipConfirmed(existing.isIdentityProviderGroupMembershipConfirmed());
    }
}
