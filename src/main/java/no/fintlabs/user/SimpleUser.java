package no.fintlabs.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

@Getter
@Setter
@Builder
public class SimpleUser {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String userType;
}
