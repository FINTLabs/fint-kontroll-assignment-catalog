package no.fintlabs.device.assignment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class NewDeviceAssignmentRequest {
    @NotNull
    Long resourceRef;
    @NotNull
    Long deviceGroupRef;
    @NotNull
    String organizationUnitId;
}
