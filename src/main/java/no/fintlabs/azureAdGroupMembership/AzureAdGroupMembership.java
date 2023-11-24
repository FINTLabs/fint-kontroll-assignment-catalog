package no.fintlabs.azureAdGroupMembership;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class AzureAdGroupMembership {
    private String id;
    private UUID azureGroupRef;
    private UUID azureUserRef;

    public AzureAdGroupMembership(String key, UUID azureUserId, UUID azureAdGroupId) {
        this.id = key;
        this.azureGroupRef = azureAdGroupId;
        this.azureUserRef = azureUserId;
    }
}
