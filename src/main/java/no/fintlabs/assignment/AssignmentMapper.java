package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;

public class AssignmentMapper {
    public static FlattenedAssignment toFlattenedAssignment(Assignment assignment) {
        return FlattenedAssignment.builder()
                .assignmentId(assignment.getId())
                .assignerRef(assignment.getAssignerRef())
                .userRef(assignment.getUserRef())
                .identityProviderUserObjectId(assignment.getAzureAdUserId())
                .resourceRef(assignment.getResourceRef())
                .resourceConsumerOrgUnitId(assignment.getResourceConsumerOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(false)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
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
                .resourceConsumerOrgUnitId(assignment.getResourceConsumerOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(false)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
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
                .resourceConsumerOrgUnitId(assignment.getResourceConsumerOrgUnitId())
                .identityProviderGroupObjectId(assignment.getAzureAdGroupId())
                .identityProviderGroupMembershipConfirmed(identityProviderGroupMembershipConfirmed)
                .identityProviderGroupMembershipDeletionConfirmed(identityProviderGroupMembershipDeletionConfirmed)
                .assignmentViaRoleRef(assignment.getRoleRef())
                .assignmentCreationDate(assignment.getAssignmentDate())
                .assignmentTerminationDate(assignment.getAssignmentRemovedDate())
                .assignmentTerminationReason(null)
                .build();
    }
}
