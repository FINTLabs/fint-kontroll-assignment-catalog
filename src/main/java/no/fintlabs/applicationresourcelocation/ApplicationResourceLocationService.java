package no.fintlabs.applicationresourcelocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationResourceLocationService {

    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final AssignmentService assignmentService;

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

    public void deleteById(Long applicationResourceLocationId) {
        Optional<ApplicationResourceLocation> applicationResourceLocation = applicationResourceLocationRepository.findById(applicationResourceLocationId);
        if (applicationResourceLocation.isEmpty()) {
            log.warn("No applicationResourceLocation found for id: {}", applicationResourceLocationId);
            return;
        }
        deleteLinkedAssignmentsForApplicationResourceLocation(applicationResourceLocation.get());
        log.info("Deleting applicationResourceLocation with id: {}", applicationResourceLocationId);
        applicationResourceLocationRepository.deleteById(applicationResourceLocationId);
    }

    private void deleteLinkedAssignmentsForApplicationResourceLocation(ApplicationResourceLocation applicationResourceLocation) {
        String orgUnitId = applicationResourceLocation.getOrgUnitId();
        Long resourceRef = applicationResourceLocation.getApplicationResourceId();
        assignmentService.deleteAssignmentsByOrgUnitIdAndResourceRef(resourceRef, orgUnitId);
    }

}
