package no.fintlabs.applicationresourcelocation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@Slf4j
public class ApplicationResourceLocationService {

    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;

    public ApplicationResourceLocationService(ApplicationResourceLocationRepository applicationResourceLocationRepository) {
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
    }

    public void save(ApplicationResourceLocation resourceLocation) {
        log.info("Saving applicationResourceLocation with id: {} - for resource: {}", resourceLocation.id, resourceLocation.resourceId);
        applicationResourceLocationRepository.save(resourceLocation);
    }
    public Optional<NearestResourceLocationDto> getNearestApplicationResourceLocationForOrgUnit(Long resourceRef, String orgUnitId) {
        log.info("Getting nearest resource location for resource: {} and user/role belonging to orgunit: {}", resourceRef, orgUnitId);

        Optional<NearestResourceLocationDto> resourceLocation = applicationResourceLocationRepository.findNearestApplicationResourceLocationForOrgUnit(resourceRef, orgUnitId)
                .stream()
                .findFirst();

        if (resourceLocation.isEmpty()) {
            log.warn("No application resource location found for resource: {} and user/role belonging to orgunit: {}", resourceRef, orgUnitId);
        }
        return resourceLocation;
    }
}
