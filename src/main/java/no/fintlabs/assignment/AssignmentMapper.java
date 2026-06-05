package no.fintlabs.assignment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssignmentMapper {
    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getEntraIdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getEntraIdGroupId())
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(null)
                .build();
    }

    public static FlattenedDeviceAssignment toFlattenedDeviceAssignment(Assignment assignment) {
        return FlattenedDeviceAssignment.builder()
                .assignmentId(assignment.getId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getEntraIdGroupId())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .terminationDate(assignment.getAssignmentRemovedDate())
                .terminationReason(null)
                .assignmentViaGroupRef(assignment.getDeviceGroupRef())
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getEntraIdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getEntraIdGroupId())
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(null)
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed, boolean identityProviderGroupMembershipDeletionConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getEntraIdUserId())
                .resourceRef(assignment.getResourceRef())
                .applicationResourceLocationOrgUnitId(assignment.getApplicationResourceLocationOrgUnitId())
                .identityProviderGroupObjectId(assignment.getEntraIdGroupId())
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(null)
                .build();
    }
}
