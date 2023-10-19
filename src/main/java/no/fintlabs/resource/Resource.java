package no.fintlabs.resource;

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
@Table(name="AssignmentResources")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class Resource {
    @Id
    private Long id;
    private String resourceId;
    private String groupObjectId;
    private UUID identityProviderGroupObjectId;
    private String resourceName;
    private String resourceType;
    @OneToMany(mappedBy = "resource",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JsonManagedReference(value="resource-assignment")
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
}