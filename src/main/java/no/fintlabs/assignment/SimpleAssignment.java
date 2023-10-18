package no.fintlabs.assignment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class SimpleAssignment {
    private Long id;
    private Long resourceRef;
    private UUID azureAdGroupId;
    private String resourceName;
    private Long userRef;
    private UUID  azureAdUserId;
    private String userDisplayname;
    private String userUsername;
    private String userType;
    private Long assignerRef;
    private String assignerDisplayname;
    private String assignerUsername;
    private Long roleRef;
    private String organizationUnitId;
}
