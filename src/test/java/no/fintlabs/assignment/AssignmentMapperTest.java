package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentMapperTest {

    @Test
    void toFlattenedAssignment() {
        Assignment assignment = AssignmentMother.createDefaultAssignment()
                .build();
        FlattenedAssignment flattenedAssignment = AssignmentMapper.toFlattenedAssignment(assignment);

        assertCommonFields(flattenedAssignment);

        assertFalse(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
        assertFalse(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
    }

    @Test
    void toFlattenedAssignmentIdentityProviderGroupMembershipConfirmed() {
        Assignment assignment = AssignmentMother.createDefaultAssignment().build();
        FlattenedAssignment flattenedAssignment = AssignmentMapper.toFlattenedAssignment(assignment, true);

        assertCommonFields(flattenedAssignment);

        assertTrue(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
        assertFalse(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
    }

    @Test
    void toFlattenedAssignmentIdentityProviderGroupMembershipDeletionConfirmed() {
        Assignment assignment = AssignmentMother.createDefaultAssignment()
                .assignmentRemovedDate(new Date())
                .build();

        FlattenedAssignment flattenedAssignment = AssignmentMapper.toFlattenedAssignment(assignment, true, true);

        assertCommonFields(flattenedAssignment);

        assertTrue(flattenedAssignment.isIdentityProviderGroupMembershipConfirmed());
        assertTrue(flattenedAssignment.isIdentityProviderGroupMembershipDeletionConfirmed());
        assertNotNull(flattenedAssignment.getAssignmentTerminationDate());
    }

    private void assertCommonFields(FlattenedAssignment flattenedAssignment) {
        assertNotNull(flattenedAssignment);
        assertEquals(111L, flattenedAssignment.getAssignmentId());
        assertEquals(222L, flattenedAssignment.getAssignerRef());
        assertEquals(333L, flattenedAssignment.getUserRef());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", flattenedAssignment.getIdentityProviderUserObjectId().toString());
        assertEquals(444L, flattenedAssignment.getResourceRef());
        assertEquals("456e4567-e89b-12d3-a456-426614174000", flattenedAssignment.getIdentityProviderGroupObjectId().toString());
        assertEquals(555L, flattenedAssignment.getAssignmentViaRoleRef());
        assertNotNull(flattenedAssignment.getAssignmentCreationDate());
        assertNull(flattenedAssignment.getAssignmentTerminationReason());
    }
}
