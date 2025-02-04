package no.fintlabs.user;

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
public class ResourceAssignmentUser {
    private Long assigneeRef;
    private String assigneeFirstName;
    private String assigneeLastName;
    private String assigneeUsername;
    private String assigneeUserType;
    private String assigneeOrganisationUnitId;
    private String assigneeOrganisationUnitName;
    private Long assignmentRef;
    private boolean directAssignment;
    private boolean deletableAssignment;
    private Long assignmentViaRoleRef;
    private String assignmentViaRoleName;
    private String assignerUsername;
    private String assignerDisplayname;
}
