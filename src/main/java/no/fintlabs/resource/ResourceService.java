package no.fintlabs.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.device.DeviceAssignmentService;
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
    private final DeviceAssignmentService deviceAssignmentService;

    public Resource save(Resource resource) {
        setDefaultStatusIfMissing(resource);
        setDefaultAssignedResourcesIfMissing(resource);
        return resourceRepository.save(resource);
    }

    public void saveUpdatedResource(Resource resource) {
        preserveAssignedResources(resource);
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

    private void setDefaultAssignedResourcesIfMissing(Resource resource) {
        if (resource.getNumberOfResourcesAssigned() == null) {
            resource.setNumberOfResourcesAssigned(0L);
        }
    }

    private void preserveAssignedResources(Resource resource) {
        resourceRepository.findById(resource.getId())
                .map(Resource::getNumberOfResourcesAssigned)
                .ifPresent(resource::setNumberOfResourcesAssigned);
    }

    private boolean isNotActive(Resource resource) {
        return !ACTIVE_STATUS.equalsIgnoreCase(resource.getStatus());
    }

    private void deactivateResourceAssignments(Resource resource) {
        log.info("Deactivating assignments for inactive resource {}, status {}",
                resource.getId(), resource.getStatus());
        assignmentService.deactivateAssignmentsByResourceId(resource.getId());
        deviceAssignmentService.deactivateAssignmentsByResourceId(resource.getId());
    }
}
