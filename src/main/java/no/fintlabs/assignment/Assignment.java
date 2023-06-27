package no.fintlabs.assignment;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name="Assignments")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class Assignment {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    private String assignmentId;
    private Long roleRef;
    private Long userRef;
    private Long resourceRef;
    private String organizationUnitId;
    private Long assignerRef;
    private Long AssignerRoleRef;
    private Date assignmentDate;
    private Date validFrom;
    private Date validTo;

    public SimpleAssignment toSimpleAssignment() {
        return SimpleAssignment
                .builder()
                .id(id)
                .resourceRef(resourceRef)
                .userRef(userRef)
                .roleRef(roleRef)
                .organizationUnitId(organizationUnitId)
                .build();
    }
    public DetailedAssignment toDetailedAssignment() {
        return DetailedAssignment
                .builder()
                .id(id)
                .resourceRef(resourceRef)
                .userRef(userRef)
                .roleRef(roleRef)
                .organizationUnitId(organizationUnitId)
                .AssignerRoleRef(assignerRef)
                .assignmentDate(assignmentDate)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }

}
