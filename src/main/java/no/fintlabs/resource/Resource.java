package no.fintlabs.resource;

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
import no.fintlabs.audit.AuditEntity;

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
@Table(name = "AssignmentResources")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Builder
public class Resource extends AuditEntity {

    @Id
    private Long id;
    private String resourceId;
    private String groupObjectId;
    private UUID identityProviderGroupObjectId;
    private String resourceName;
    private String resourceType;
    private String licenseEnforcement;
    @Column(name="number_of_resources_assigned")
    private Long numberOfResourcesAssigned;
    private Long resourceLimit;

    @OneToMany(mappedBy = "resource",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value = "resource-assignment")
    @JsonIgnore
    @ToString.Exclude
    private Set<Assignment> assignments = new HashSet<>();

    public AssignmentResource toSimpleResource() {
        return AssignmentResource
                .builder()
                .id(id)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .resourceType(resourceType)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Resource resource = (Resource) o;
        return Objects.equals(id, resource.id) && Objects.equals(resourceId, resource.resourceId) && Objects.equals(groupObjectId, resource.groupObjectId) &&
               Objects.equals(identityProviderGroupObjectId, resource.identityProviderGroupObjectId) && Objects.equals(resourceName, resource.resourceName) &&
               Objects.equals(resourceType, resource.resourceType) && Objects.equals(licenseEnforcement, resource.licenseEnforcement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resourceId, groupObjectId, identityProviderGroupObjectId, resourceName, resourceType, licenseEnforcement);
    }
}
