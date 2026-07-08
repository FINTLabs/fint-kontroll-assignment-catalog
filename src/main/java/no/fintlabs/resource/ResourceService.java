package no.fintlabs.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceService {
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ResourceRepository resourceRepository;
    private final AssignmentService assignmentService;

    public Resource save(Resource resource) {
        setDefaultStatusIfMissing(resource);
        return resourceRepository.save(resource);
    }

    public void saveUpdatedResource(Resource resource) {
        Resource savedResource = save(resource);
        if (isNotActive(savedResource)) {
            deactivateResourceAssignments(savedResource);
        }
    }

    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    public int deactivateAssignmentsForInactiveResources() {
        List<Resource> inactiveResources = findAll()
                .stream()
                .filter(this::isNotActive)
                .toList();

        inactiveResources.forEach(this::deactivateResourceAssignments);

        return inactiveResources.size();
    }

    private void setDefaultStatusIfMissing(Resource resource) {
        if (resource.getStatus() == null) {
            resource.setStatus(DEFAULT_STATUS);
        }
    }

    private boolean isNotActive(Resource resource) {
        return !ACTIVE_STATUS.equalsIgnoreCase(resource.getStatus());
    }

    private void deactivateResourceAssignments(Resource resource) {
        log.info("Deactivating assignments for inactive resource {}, status {}",
                resource.getId(), resource.getStatus());
        assignmentService.deactivateAssignmentsByResourceId(resource.getId());
    }
}
