package no.fintlabs.groupmembership;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class EntraIdGroupMembership {
    private EntraStatus code;
    private UUID entraGroupRef;
    private UUID entraUserRef;

    @JsonCreator
    public EntraIdGroupMembership(@JsonProperty("code") EntraStatus code,
                                  @JsonProperty("entraGroupRef") UUID entraGroupRef,
                                  @JsonProperty("entraUserRef") UUID entraUserRef) {
        this.code = code;
        this.entraGroupRef = entraGroupRef;
        this.entraUserRef = entraUserRef;
    }

    @JsonProperty("entraGroupRef")
    public UUID getEntraGroupRef() {
        return entraGroupRef;
    }

    @JsonProperty("entraUserRef")
    public UUID getEntraUserRef() {
        return entraUserRef;
    }
}
