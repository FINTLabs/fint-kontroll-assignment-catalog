package no.fintlabs.opa;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.model.OrgUnitType;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
public class OpaUtils {
    public static List<String> getOrgUnitsToFilter(List<String> orgUnits, List<String> orgUnitsInScope) {

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
    public static boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }
}
