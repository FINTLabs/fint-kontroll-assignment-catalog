package no.fintlabs.assignment;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;

@Getter
public class NewAssignmentRequest {
    @NotNull
    Long resourceRef;
    Long userRef;
    Long roleRef;
    @NotNull
    String organizationUnitId;
}
