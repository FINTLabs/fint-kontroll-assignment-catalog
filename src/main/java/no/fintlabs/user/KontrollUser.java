package no.fintlabs.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Slf4j
public class KontrollUser {
    private Long id;
    private String resourceId;
    private String firstName;
    private String lastName;
    private String userType;
    private String userName;
    private UUID identityProviderUserObjectId;
    private String mainOrganisationUnitName;
    private String mainOrganisationUnitId;
    private String status;
    private Date statusChanged;
}
