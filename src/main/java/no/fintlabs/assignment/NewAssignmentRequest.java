package no.fintlabs.assignment;

import lombok.Getter;

@Getter
public class NewAssignmentRequest {
    Long resourceRef;
    Long userRef;
    String organizationUnitId;
}
