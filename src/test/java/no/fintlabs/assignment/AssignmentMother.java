package no.fintlabs.assignment;

import java.util.Date;
import java.util.UUID;

public class AssignmentMother {
    public static Assignment.AssignmentBuilder createDefaultAssignment() {
        return Assignment.builder()
                .id(111L)
                .assignerRef(222L)
                .userRef(333L)
                .azureAdUserId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .resourceRef(444L)
                .azureAdGroupId(UUID.fromString("456e4567-e89b-12d3-a456-426614174000"))
                .roleRef(555L)
                .assignmentDate(new Date())
                .validTo(new Date());
    }
}
