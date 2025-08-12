package no.fintlabs.applicationresourcelocation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@Transactional
public class ApplicationResourceLocationService {

    private final ApplicationResourceLocationRepository applicationResourceLocationRepository;
    private final AssignmentService assignmentService;

    public ApplicationResourceLocationService(ApplicationResourceLocationRepository applicationResourceLocationRepository, @Lazy AssignmentService assignmentService) {
        this.applicationResourceLocationRepository = applicationResourceLocationRepository;
        this.assignmentService = assignmentService;
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

    public void deleteById(Long applicationResourceLocationId) {
        Optional<ApplicationResourceLocation> applicationResourceLocation = applicationResourceLocationRepository.findById(applicationResourceLocationId);
        if (applicationResourceLocation.isEmpty()) {
            log.warn("No applicationResourceLocation found for id: {}", applicationResourceLocationId);
            return;
        }
        log.info("Deleting applicationResourceLocation with id: {}", applicationResourceLocationId);
        applicationResourceLocationRepository.deleteById(applicationResourceLocationId);
        reassignLinkedAssignments(applicationResourceLocation.get());

    }

    private void reassignLinkedAssignments(ApplicationResourceLocation applicationResourceLocation) {
        String orgUnitId = applicationResourceLocation.getOrgUnitId();
        Long resourceRef = applicationResourceLocation.getApplicationResourceId();
        assignmentService.reassignLinkedAssignments(resourceRef, orgUnitId);
    }

}
