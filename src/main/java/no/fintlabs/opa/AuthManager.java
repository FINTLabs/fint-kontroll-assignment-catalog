package no.fintlabs.opa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AuthManager {
    @Value("${fint.kontroll.authorization.authorized-role:rolle}")
    private String authorizedRole;

    @Value("${fint.kontroll.authorization.authorized-admin-role:admin}")
    private String adminRole;

    @Value("${fint.kontroll.authorization.authorized-org-id:vigo.no}")
    private String authorizedOrgId;

    public boolean hasAdminAdminAccess(Jwt jwtToken) {
        List<String> roles = jwtToken.getClaim("roles");

        if (roles != null) {
            roles.forEach(r -> log.info("Roles in token attributes: {}", r));
        }

        boolean isAdmin = roles != null && roles.contains("authenticated") && roles.contains(adminRole);
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
