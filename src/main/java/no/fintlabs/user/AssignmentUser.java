package no.fintlabs.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;

@Getter
@Setter
@Builder
public class AssignmentUser {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String userType;
    private Long assignmentRef;
    private String assignerUsername;
    private String assignerDisplayname;
    private String organisationUnitId;
    private String organisationUnitName;
}
