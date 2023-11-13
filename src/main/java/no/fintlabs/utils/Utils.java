package no.fintlabs.utils;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.AuthorizationClient;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.opa.model.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Utils {
    private final AuthorizationClient authorizationClient;

    public Utils(AuthorizationClient authorizationClient) {
        this.authorizationClient = authorizationClient;
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
    public List<String> getOrgUnitsToFilter(List<String> orgUnits, List<String> orgUnitsInScope) {

        if (orgUnits == null) {
            log.info("OrgUnits parameter is empty, using orgunits from scope {} in search", orgUnitsInScope);
            return orgUnitsInScope;
        }
        log.info("OrgUnits parameter list: {}", orgUnits);

        if (orgUnitsInScope.contains(OrgUnitType.ALLORGUNITS.name())) {
            return orgUnits;
        }
        List<String> filteredOrgUnits = orgUnits.stream()
                .filter(orgUnitsInScope::contains)
                .collect(Collectors.toList());

        log.info("OrgUnits in search: {}", filteredOrgUnits);
        return filteredOrgUnits;
    }
    public boolean isEmptyString(String string) {
        return string == null || string.length() == 0;
    }
}
