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

    private final ResourceRepository resourceRepository;
    private final AssignmentService assignmentService;

    public Resource save(Resource resource) {
        setDefaultStatusIfMissing(resource);
        return resourceRepository.save(resource);
    }

    public Resource saveUpdatedResource(Resource resource) {
        Resource savedResource = save(resource);
        if (!isActive(savedResource)) {
            assignmentService.deactivateAssignmentsByResourceId(savedResource.getId());
        }
        return savedResource;
    }

    public List<Resource> findAll() {
        return resourceRepository.findAll();
    }

    public int deactivateAssignmentsForInactiveResources() {
        List<Resource> inactiveResources = findAll()
                .stream()
                .filter(resource -> !isActive(resource))
                .toList();

        inactiveResources.forEach(resource -> assignmentService.deactivateAssignmentsByResourceId(resource.getId()));

        return inactiveResources.size();
    }

    private void setDefaultStatusIfMissing(Resource resource) {
        if (resource.getStatus() == null) {
            resource.setStatus(DEFAULT_STATUS);
        }
    }

    private boolean isActive(Resource resource) {
        return DEFAULT_STATUS.equalsIgnoreCase(resource.getStatus());
    }
}
