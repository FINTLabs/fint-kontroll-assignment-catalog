package no.fintlabs.user;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.annotate.JsonIgnore;
import no.fintlabs.assignment.Assignment;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name="Users")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class User {
    @Id
    private Long id;
    private Long userRef;
    private String userObjectId;
    private String firstName;
    private String lastName;
    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonIgnore
    @ToString.Exclude
    private Set<Assignment> assignments = new HashSet<>();

}

