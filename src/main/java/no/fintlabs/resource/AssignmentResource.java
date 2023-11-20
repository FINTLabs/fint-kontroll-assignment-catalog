package no.fintlabs.resource;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Id;

@Getter
@Setter
@Builder
public class AssignmentResource {
    @Id
    private Long id;
    private String resourceId;
    private String resourceName;
    private String resourceType;
    private Long assignmentRef;
}
