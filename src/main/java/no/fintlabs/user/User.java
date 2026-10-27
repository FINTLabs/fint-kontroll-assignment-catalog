package no.fintlabs.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@Slf4j
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Builder
public class User {

    @Id
    private Long id;
    private String userName;
    private UUID identityProviderUserObjectId;
    private String firstName;
    private String lastName;
    private String userType;
    private String organisationUnitId;
    private String organisationUnitName;
    @Column(nullable = false)
    private String status;
    private Date statusChanged;
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;
    @UpdateTimestamp
    private Instant modifiedDate;

    @OneToMany(mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value = "user-assignment")
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private Set<Assignment> assignments = new HashSet<>();

    public String getDisplayname() {
        if (!stringIsNullOrEmpty(firstName) && !stringIsNullOrEmpty(lastName)) {
            return firstName + " " + lastName;
        }
        return null;
    }

    private boolean stringIsNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public boolean hasSamePersistedStateAs(User other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        return Objects.equals(id, other.id) &&
               Objects.equals(userName, other.userName) &&
               Objects.equals(identityProviderUserObjectId, other.identityProviderUserObjectId) &&
               Objects.equals(firstName, other.firstName) &&
               Objects.equals(lastName, other.lastName) &&
               Objects.equals(userType, other.userType) &&
               Objects.equals(organisationUnitId, other.organisationUnitId) &&
               Objects.equals(organisationUnitName, other.organisationUnitName) &&
               Objects.equals(status, other.status) &&
               Objects.equals(statusChanged, other.statusChanged);
    }
}

