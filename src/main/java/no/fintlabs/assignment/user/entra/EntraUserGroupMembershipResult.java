package no.fintlabs.assignment.user.entra;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.entra.membership.EntraReturnCode;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;

import java.util.UUID;
@Getter
@Setter
public class EntraUserGroupMembershipResult {
    private EntraReturnCode code;
    private UUID entraGroupRef;
    private UUID entraUserRef;

    @JsonCreator
    public EntraUserGroupMembershipResult(@JsonProperty("code") EntraReturnCode code,
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
