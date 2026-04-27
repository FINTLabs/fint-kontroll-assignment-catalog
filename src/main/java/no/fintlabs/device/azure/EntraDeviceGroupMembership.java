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
public class EntraDeviceGroupMembership {
    private EntraReturnCode code;
    private UUID entraResourceRef;
    private UUID entraDeviceRef;

    @JsonCreator
    public EntraDeviceGroupMembership(@JsonProperty("code") EntraReturnCode code,
                                      @JsonProperty("entraResourceRef") UUID entraResourceRef,
                                      @JsonProperty("entraDeviceRef") UUID entraDeviceRef) {
        this.code = code;
        this.entraResourceRef = entraResourceRef;
        this.entraDeviceRef = entraDeviceRef;
    }
}
