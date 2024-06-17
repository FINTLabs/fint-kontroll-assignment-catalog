package no.fintlabs.role;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.annotate.JsonIgnore;
import no.fintlabs.assignment.Assignment;

import java.util.HashSet;
import java.util.Objects;
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
@Builder
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Role role = (Role) o;
        return Objects.equals(id, role.id) && Objects.equals(roleObjectId, role.roleObjectId) && Objects.equals(roleName, role.roleName) &&
               Objects.equals(roleType, role.roleType) && Objects.equals(organisationUnitId, role.organisationUnitId) &&
               Objects.equals(organisationUnitName, role.organisationUnitName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleObjectId, roleName, roleType, organisationUnitId, organisationUnitName);
    }
}
