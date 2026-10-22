package no.fintlabs.entra.membership.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserResourceGroupMembershipCommand {
    private OperationType operation;
    private UUID entraGroupRef;
    private UUID entraUserRef;

    @JsonCreator
    public UserResourceGroupMembershipCommand(@JsonProperty("operation") OperationType operation,
                                   @JsonProperty("entraGroupRef") UUID entraGroupRef,
                                   @JsonProperty("userGroupRef") UUID entraUserRef) {
        this.operation = operation;
        this.entraGroupRef = entraGroupRef;
        this.entraUserRef = entraUserRef;
    }

    @JsonProperty("entraGroupRef")
    public UUID getEntraGroupRef() {
        return entraGroupRef;
    }

    @JsonProperty("userGroupRef")
    public UUID getEntraUserRef() {
        return entraUserRef;
    }
}
