package no.fintlabs.assignment;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailedAssignment {
    private Long id;
    private Long resourceRef;
    private String resourceName;
    private Long userRef;
    private String userFirstName;
    private String userLastName;
    private String userType;
    private Long roleRef;
    private String organizationUnitId;
    private Long AssignerRoleRef;
    private Date assignmentDate;
    private Date validFrom;
    private Date validTo;
}
