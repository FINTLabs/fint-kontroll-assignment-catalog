package no.fintlabs.role;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;

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
