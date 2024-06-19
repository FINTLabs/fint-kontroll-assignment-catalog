package no.fintlabs.user;

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
import java.util.UUID;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name = "Users")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Builder
public class User {

    @Id
    private Long id;
    private Long userRef;
    private String userObjectId;
    private String userName;
    private UUID identityProviderUserObjectId;
    private String firstName;
    private String lastName;
    private String userType;
    //private String displayName;
    private String organisationUnitId;
    private String organisationUnitName;

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value = "user-assignment")
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
        return assignments.stream()
                .filter(assignment -> assignment.getResourceRef().equals(resourceRef))
                .map(Assignment::getId)
                .toList().get(0);
    }

    public String getDisplayname() {
        if (!stringIsNullOrEmpty(firstName) && !stringIsNullOrEmpty(lastName)) {
            return firstName + " " + lastName;
        }
        return null;
    }

    private boolean stringIsNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(userRef, user.userRef) &&
               Objects.equals(userObjectId, user.userObjectId) &&
               Objects.equals(userName, user.userName) &&
               Objects.equals(identityProviderUserObjectId, user.identityProviderUserObjectId) &&
               Objects.equals(firstName, user.firstName) &&
               Objects.equals(lastName, user.lastName) &&
               Objects.equals(userType, user.userType) &&
               Objects.equals(organisationUnitId, user.organisationUnitId) &&
               Objects.equals(organisationUnitName, user.organisationUnitName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userRef, userObjectId, userName, identityProviderUserObjectId, firstName, lastName, userType, organisationUnitId, organisationUnitName);
    }
}

