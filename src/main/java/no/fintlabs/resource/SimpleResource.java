package no.fintlabs.resource;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

@Getter
@Setter
@Builder
public class SimpleResource {
    @Id
    private Long id;
    private String resourceId;
    private String resourceName;
    private String resourceType;
}
