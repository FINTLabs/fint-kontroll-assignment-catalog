package no.fintlabs.assignment;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
    private Long roleRef;
    private Long userRef;
    private Long resourceRef;
    private Long assignerRef;
    private Long AssignerRoleRef;
    private Date assignmentDate;
    private Date validFrom;
    private Date validTo;

}
