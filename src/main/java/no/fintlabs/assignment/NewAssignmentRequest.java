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
    Long deviceGroupRef;
    @NotBlank
    String organizationUnitId;

    @AssertTrue(message = "Exactly one of userRef, roleRef or deviceGroupRef must be set")
    public boolean isExactlyOneAssigneeRefSet() {
        int assigneeCount = 0;
        assigneeCount += userRef == null ? 0 : 1;
        assigneeCount += roleRef == null ? 0 : 1;
        assigneeCount += deviceGroupRef == null ? 0 : 1;
        return assigneeCount == 1;
    }
}
