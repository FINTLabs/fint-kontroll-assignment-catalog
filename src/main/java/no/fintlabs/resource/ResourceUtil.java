package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;

import java.util.List;

@Slf4j
public class ResourceUtil {

    public static boolean isResourceLocationInScope(Assignment assignment, List<String> orgUnitsInScope) {
        try {
            boolean isApplicationResourceLocationInScope =
                    assignment.getApplicationResourceLocationOrgUnitId() != null &&
                            orgUnitsInScope.contains(assignment.getApplicationResourceLocationOrgUnitId());
            log.info("Resource location {} in scope for assignment {} is: {}",
                    assignment.getApplicationResourceLocationOrgUnitId(),
                    assignment.getId(),
                    isApplicationResourceLocationInScope);

            return isApplicationResourceLocationInScope;
        }
        catch (Exception e)
        {
            log.error("Calculation of isResourceLocationInScope for assignment {} failed with error {}",
                    assignment.getId(),
                    e.getMessage());
            return false;
        }
    }

    public static boolean isResourceUnrestricted(Resource resource) {
        //TODO: temporary solution, should be replaced with a proper check
        if (resource.getLicenseEnforcement() == null) {
            return true;
        }
        List<String> unrestrictedEnforcementTypes = List.of(
                Handhevingstype.NOTSPECIFIED.name(),
                Handhevingstype.NOTSET.name(),
                Handhevingstype.FREEALL.name(),
                Handhevingstype.FREEEDU.name(),
                Handhevingstype.FREESTUDENT.name());
        boolean isResourceUnrestricted = unrestrictedEnforcementTypes.contains(resource.getLicenseEnforcement());
        log.info("Resource {} is unrestricted: {}", resource.getId(), isResourceUnrestricted);
        return isResourceUnrestricted;
    }
}
