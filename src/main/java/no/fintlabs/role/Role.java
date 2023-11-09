package no.fintlabs.role;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.annotate.JsonIgnore;
import no.fintlabs.assignment.Assignment;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name="AssignmentRoles")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class Role {
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roleObjectId;
    private String roleName;
    private String roleType;
    private String organisationUnitId;
    private String organisationUnitName;
    @OneToMany(mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value="role-assignment")
    @JsonIgnore
    @ToString.Exclude
    private Set<Assignment> assignments = new HashSet<>();

    public AssignmentRole toAssignmentRole() {
        return AssignmentRole
                .builder()
                .id(id)
                .roleName(roleName)
                .roleType(roleType)
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
