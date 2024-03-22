package no.fintlabs.assignment;

import java.util.Date;
import java.util.UUID;

public class AssignmentMapper {
    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .assignerRef(assignment.getAssignerRef())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(false)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getValidTo())
                .assignmentTerminationReason(null)
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .assignerRef(assignment.getAssignerRef())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getValidTo())
                .assignmentTerminationReason(null)
                .build();
    }

    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment, boolean identityProviderGroupMembershipConfirmed, boolean identityProviderGroupMembershipDeletionConfirmed) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .assignerRef(assignment.getAssignerRef())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(identityProviderGroupMembershipDeletionConfirmed)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getValidTo())
                .assignmentTerminationReason(null)
                .build();
    }
}
