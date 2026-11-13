package no.fintlabs.assignment;

import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignment;
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

        assertNull(flattenedAssignment.getUserEntraMembership());
        assertNull(flattenedAssignment.getAssignmentTerminationReason());
    }

    @Test
    void toFlattenedAssignmentTerminatesRemovedAssignment() {
        Assignment assignment = AssignmentMother.createDefaultAssignment()
                .assignmentRemovedDate(new Date())
                .build();

        FlattenedAssignment flattenedAssignment = AssignmentMapper.toFlattenedAssignment(assignment);

        assertCommonFields(flattenedAssignment);

        assertNotNull(flattenedAssignment.getAssignmentTerminationDate());
        assertEquals("Assignment has been terminated", flattenedAssignment.getAssignmentTerminationReason());
    }

    @Test
    void toFlattenedDeviceAssignment_shouldSetReasonWhenTerminationDateIsSet() {
        Assignment assignment = AssignmentMother.createDefaultAssignment()
                .assignmentRemovedDate(new Date())
                .build();

        FlattenedDeviceAssignment flattenedAssignment = AssignmentMapper.toFlattenedDeviceAssignment(assignment);

        assertNotNull(flattenedAssignment.getTerminationDate());
        assertEquals("Assignment has been terminated", flattenedAssignment.getTerminationReason());
    }

    private void assertCommonFields(FlattenedAssignment flattenedAssignment) {
        assertNotNull(flattenedAssignment);
        assertEquals(111L, flattenedAssignment.getAssignmentId());
        assertEquals(333L, flattenedAssignment.getUserRef());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", flattenedAssignment.getIdentityProviderUserObjectId().toString());
        assertEquals(444L, flattenedAssignment.getResourceRef());
        assertEquals("456e4567-e89b-12d3-a456-426614174000", flattenedAssignment.getIdentityProviderGroupObjectId().toString());
        assertEquals(555L, flattenedAssignment.getAssignmentViaRoleRef());
        assertNotNull(flattenedAssignment.getAssignmentCreationDate());
    }
}
