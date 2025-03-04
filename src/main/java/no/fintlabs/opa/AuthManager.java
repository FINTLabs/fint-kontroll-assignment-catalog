package no.fintlabs.opa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthManager {
    @Value("${fint.kontroll.authorization.authorized-org-id:vigo.no}")
    private String authorizedOrgId;

    private final AuthorizationClient authorizationClient;

    public AuthManager(AuthorizationClient authorizationClient) {
        this.authorizationClient = authorizationClient;
    }

    public boolean hasAdminAdminAccess(Jwt jwtToken) {
        boolean isAdmin = authorizationClient.isAdmin();
        boolean hasCorrectOrgId = hasCorrectOrgId(jwtToken);

        return hasCorrectOrgId && isAdmin;
    }

    private boolean hasCorrectOrgId(Jwt jwtToken) {
        String orgId = jwtToken.getClaim("organizationid");
        if (orgId != null) {
            log.info("OrgId in token attributes: {}", orgId);
        }

        return orgId != null && orgId.equals(authorizedOrgId);
    }
}
