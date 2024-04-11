package no.fintlabs.azureadgroupmembership;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class AzureAdGroupMembership {
    private String id;
    @JsonProperty("group_id")
    private UUID azureGroupRef;

    @JsonProperty("user_id")
    private UUID azureUserRef;

    public AzureAdGroupMembership(String key, UUID azureAdGroupId, UUID azureUserId) {
        this.id = key;
        this.azureGroupRef = azureAdGroupId;
        this.azureUserRef = azureUserId;
    }
}
