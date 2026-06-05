package no.fintlabs.assignment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class NewAssignmentRequest {
    @NotNull
    Long resourceRef;
    Long userRef;
    Long roleRef;
    @NotBlank
    String organizationUnitId;

    @AssertTrue(message = "Exactly one of userRef or roleRef must be set")
    public boolean isExactlyOneAssigneeRefSet() {
        return userRef != null ^ roleRef != null;
    }
}
