package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AssignmentResourceService {
    private final ResourceRepository resourceRepository;
    private final AssignmentRepository assignmentRepository;

    public AssignmentResourceService(ResourceRepository resourceRepository, AssignmentRepository assignmentRepository) {
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<AssignmentResource> getResourcesAssignedToUser(Long userId) {
        List<AssignmentResource> resources = resourceRepository
                .getResourcesByUserId(userId)
                .stream()
                .map(Resource::toSimpleResource)
                .map(resource ->  {
                    resource.setAssignmentRef(getAssignmentRef(userId, resource.getId()));
                    return resource;
                })
                .toList();
        return resources;

    }
    private Long getAssignmentRef(Long userId, Long resourceId) {
        return assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId).getId();
    }
}
