package no.fintlabs.assignment;

import lombok.RequiredArgsConstructor;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AssignmentResourceEnricher {

    private final ApplicationResourceLocationService applicationResourceLocationService;

    public void enrich(Assignment assignment, Long resourceRef, Resource resource) {
        assignment.setResourceName(resource.getResourceName());
        assignment.setEntraGroupId(resource.getIdentityProviderGroupObjectId());

        Optional<NearestResourceLocationDto> nearestApplicationResourceLocationDto =
                applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(
                        resourceRef,
                        assignment.getOrganizationUnitId());

        nearestApplicationResourceLocationDto.ifPresent(nearestApplicationResourceLocation -> {
            assignment.setApplicationResourceLocationOrgUnitId(nearestApplicationResourceLocation.orgUnitId());
            assignment.setApplicationResourceLocationOrgUnitName(nearestApplicationResourceLocation.orgUnitName());
        });
    }
}
