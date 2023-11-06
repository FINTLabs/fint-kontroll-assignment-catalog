package no.fintlabs.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.annotate.JsonIgnore;
import no.fintlabs.assignment.Assignment;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    private UUID identityProviderUserObjectId;
    private String firstName;
    private String lastName;
    private String userType;
    private String organisationUnitId;
    private String organisationUnitName;
    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value="user-assignment")
    @JsonIgnore
    @ToString.Exclude
    private Set<Assignment> assignments = new HashSet<>();

    public AssignmentUser toAssignmentUser() {
        return AssignmentUser
                .builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .userType(userType)
                .organisationUnitId(organisationUnitId)
                .organisationUnitName(organisationUnitName)
                .build();
    }

    private Long getAssignmentId(Long resourceRef) {
        return  assignments.stream()
                .filter(assignment -> assignment.getResourceRef().equals(resourceRef))
                .map(assignment -> assignment.getId())
                .toList().get(0);
    }
}

