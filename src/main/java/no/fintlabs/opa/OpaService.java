package no.fintlabs.opa;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.model.Scope;
import no.fintlabs.util.AuthenticationUtil;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpaService {
    private final AuthorizationClient authorizationClient;
    private final AuthenticationUtil authenticationUtil;

    public OpaService(AuthorizationClient authorizationClient, AuthenticationUtil authenticationUtil) {
        this.authorizationClient = authorizationClient;
        this.authenticationUtil = authenticationUtil;
    }

    public List<String> getOrgUnitsInScope() {

        List<Scope> userScopes = authorizationClient.getUserScopes();
        log.info("User scopes from api: {}", userScopes);

        return userScopes.stream()
                .filter(scope -> scope.getObjectType().equals("role"))
                .map(Scope::getOrgUnits)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
    public String getUserNameAuthenticatedUser() {
        return authenticationUtil.getUserName();
    }
}
