package no.fintlabs.assignment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleAssignment {
    private Long id;
    private Long resourceRef;
    private String resourceName;
    private Long userRef;
    private String userDisplayname;
    private String userUsername;
    private String userType;
    private Long assignerRef;
    private String assignerDisplayname;
    private String assignerUsername;
    private Long roleRef;
    private String organizationUnitId;
}
