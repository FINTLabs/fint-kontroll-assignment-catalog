package no.fintlabs.role;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

@Getter
@Setter
@Builder
public class AssignmentRole {
    @Id
    private Long id;
    private String roleName;
    private String roleType;
    private Long assignmentRef;
    private String organisationUnitId;
    private String organisationUnitName;
}
