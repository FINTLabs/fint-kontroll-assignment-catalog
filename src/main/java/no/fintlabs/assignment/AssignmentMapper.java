package no.fintlabs.assignment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssignmentMapper {
    private static final String ASSIGNMENT_TERMINATED_REASON = "Assignment has been terminated";

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(false)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(getTerminationReason(assignment))
                .build();
    }

    public static FlattenedDeviceAssignment toFlattenedDeviceAssignment(Assignment assignment) {
        return FlattenedDeviceAssignment.builder()
                .assignmentId(assignment.getId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .terminationDate(assignment.getAssignmentRemovedDate())
                .terminationReason(getTerminationReason(assignment))
                .assignmentViaGroupRef(assignment.getDeviceGroupRef())
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(getTerminationReason(assignment))
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed, boolean identityProviderGroupMembershipDeletionConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(identityProviderGroupMembershipDeletionConfirmed)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(getTerminationReason(assignment))
                .build();
    }

    private static String getTerminationReason(Assignment assignment) {
        return assignment.getAssignmentRemovedDate() == null ? null : ASSIGNMENT_TERMINATED_REASON;
    }
}
