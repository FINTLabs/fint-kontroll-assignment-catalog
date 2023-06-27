package no.fintlabs.assignment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleAssignment {
    private Long id;
    private Long resourceRef;
    private Long userRef;
    private Long roleRef;
    private String organizationUnitId;
}
