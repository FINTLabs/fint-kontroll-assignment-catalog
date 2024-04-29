package no.fintlabs.groupmembership;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class AzureAdGroupMembership {
    private String id;
    private UUID azureGroupRef;
    private UUID azureUserRef;

    @JsonCreator
    public AzureAdGroupMembership(@JsonProperty("id") String id,
                                  @JsonProperty("group_id") UUID azureGroupRef,
                                  @JsonProperty("user_id") UUID azureUserRef) {
        this.id = id;
        this.azureGroupRef = azureGroupRef;
        this.azureUserRef = azureUserRef;
    }
}
