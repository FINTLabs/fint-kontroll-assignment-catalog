package no.fintlabs.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAssignmentResource {
    private Long assigneeRef;
    private Long assignmentRef;
    private boolean directAssignment;
    private boolean deletableAssignment;
    private Long assignmentViaRoleRef;
    private String assignmentViaRoleName;
    private String assignerUsername;
    private String assignerDisplayname;
    private Long resourceRef;
    private String resourceName;
    private String resourceType;
    private String licenseEnforcement;
}
