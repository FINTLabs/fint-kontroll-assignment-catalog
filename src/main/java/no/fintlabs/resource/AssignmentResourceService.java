package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId);

        if (assignment.isPresent()) {
            //return Optional.of(assignment.get().getId());
            return assignment.get().getId();
        }
        return null;
    }
}
