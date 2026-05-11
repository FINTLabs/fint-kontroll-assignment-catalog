package no.fintlabs.device.entra;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class EntraDeviceGroupMembership {
    private EntraReturnCode code;
    private UUID entraGroupRef;
    private UUID entraDeviceRef;

    @JsonCreator
    public EntraDeviceGroupMembership(@JsonProperty("code") EntraReturnCode code,
                                      @JsonProperty("entraGroupRef") UUID entraGroupRef,
                                      @JsonProperty("entraDeviceRef") UUID entraDeviceRef) {
        this.code = code;
        this.entraGroupRef = entraGroupRef;
        this.entraDeviceRef = entraDeviceRef;
    }
}
