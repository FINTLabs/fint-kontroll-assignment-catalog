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
    private String resourceRef;
    private String userRef;
    private String roleRef;
    private String organizationUnitId;
    private Long AssignerRoleRef;
    private Date assignmentDate;
    private Date validFrom;
    private Date validTo;
}
