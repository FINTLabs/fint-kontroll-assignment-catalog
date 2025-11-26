package no.fintlabs.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeviceResourceGroupMembership {
    private OperationType operation;
    private UUID azureGroupRef;
    private UUID azureDeviceRef;

    @JsonCreator
    public DeviceResourceGroupMembership(OperationType operation, UUID azureGroupRef, UUID azureDeviceRef) {
        this.operation = operation;
        this.azureGroupRef = azureGroupRef;
        this.azureDeviceRef = azureDeviceRef;
    }
}
