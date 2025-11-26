package no.fintlabs.device.azure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AzureAdDeviceGroupMembership {
    private AzureReturnCode code;
    private UUID azureResourceRef;
    private UUID azureDeviceRef;

    @JsonCreator
    public AzureAdDeviceGroupMembership(@JsonProperty("code") AzureReturnCode code,
                                        @JsonProperty("azureResourceRef") UUID azureResourceRef,
                                        @JsonProperty("azureDeviceRef") UUID azureDeviceRef) {
        this.code = code;
        this.azureResourceRef = azureResourceRef;
        this.azureDeviceRef = azureDeviceRef;
    }
}
