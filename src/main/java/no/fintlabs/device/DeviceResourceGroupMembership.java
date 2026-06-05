package no.fintlabs.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.fintlabs.groupmembership.OperationType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeviceResourceGroupMembership {
    private OperationType operation;
    private UUID entraGroupRef;
    private UUID entraDeviceRef;

    @JsonCreator
    public DeviceResourceGroupMembership(OperationType operation, UUID entraGroupRef, UUID entraDeviceRef) {
        this.operation = operation;
        this.entraGroupRef = entraGroupRef;
        this.entraDeviceRef = entraDeviceRef;
    }
}
