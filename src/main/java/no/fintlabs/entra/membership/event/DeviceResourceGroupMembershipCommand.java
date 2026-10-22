package no.fintlabs.entra.membership.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.fintlabs.entra.membership.event.OperationType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeviceResourceGroupMembershipCommand {
    private OperationType operation;
    private UUID entraGroupRef;
    private UUID entraDeviceRef;

    @JsonCreator
    public DeviceResourceGroupMembershipCommand(OperationType operation, UUID entraGroupRef, UUID entraDeviceRef) {
        this.operation = operation;
        this.entraGroupRef = entraGroupRef;
        this.entraDeviceRef = entraDeviceRef;
    }
}
