package no.fintlabs.groupmembership;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ResourceGroupMembership {
    private String id;
    private UUID azureGroupRef;
    private UUID azureUserRef;

    @JsonCreator
    public ResourceGroupMembership(String id, UUID azureGroupRef, UUID azureUserRef) {
        this.id = id;
        this.azureGroupRef = azureGroupRef;
        this.azureUserRef = azureUserRef;
    }
}
