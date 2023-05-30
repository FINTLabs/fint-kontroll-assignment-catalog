package no.fintlabs.assignment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SimpleAssignment {
    private Long id;
    private String resourceRef;
    private String userRef;
    private String roleRef;
}
