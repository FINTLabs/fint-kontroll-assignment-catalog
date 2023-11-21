package no.fintlabs.azureAdGroupMembership;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class AzureAdGroupMembership {
    private String id;
    private UUID resourceRef;
    private UUID userRef;

    public AzureAdGroupMembership(String key, UUID azureUserId, UUID azureAdGroupId) {
    }
}
