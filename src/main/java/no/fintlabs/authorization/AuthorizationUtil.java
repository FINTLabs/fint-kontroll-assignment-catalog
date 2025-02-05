package no.fintlabs.authorization;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.AuthorizationClient;
import no.fintlabs.opa.model.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class AuthorizationUtil {

    private final AuthorizationClient authorizationClient;

    public AuthorizationUtil(AuthorizationClient authorizationClient) {
        this.authorizationClient = authorizationClient;
    }
    public List<String> getAllAuthorizedOrgUnitIDs() {
        List<Scope> scopes = authorizationClient.getUserScopesList();
        List<String> authorizedOrgUnitIDs = scopes.stream()
                .filter(s -> s.getObjectType().equals("resource"))
                .map(Scope::getOrgUnits)
                .flatMap(Collection::stream)
                .toList();
        log.info("Authorized orgUnitIDs : " + authorizedOrgUnitIDs);
        return authorizedOrgUnitIDs;
    }
}
