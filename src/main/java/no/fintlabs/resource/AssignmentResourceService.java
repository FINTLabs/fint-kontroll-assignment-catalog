package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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

    public Page<AssignmentResource> getResourcesAssignedToUser(Long userId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec,page)
                .map(Resource::toSimpleResource)
                .map(resource ->  {
                    resource.setAssignmentRef(getAssignmentRefForUser(userId, resource.getId()));
                    return resource;
                });
        return resources;
    }
    public Page<AssignmentResource> getResourcesAssignedToRole(Long roleId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec,page)
                .map(Resource::toSimpleResource)
                .map(resource ->  {
                    resource.setAssignmentRef(getAssignmentRefForRole(roleId, resource.getId()));
                    return resource;
                });
        return resources;
    }
    private Long getAssignmentRefForUser(Long userId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId);

        if (assignment.isPresent()) {
            //return Optional.of(assignment.get().getId());
            return assignment.get().getId();
        }
        return null;
    }
    //findAssignmentByRoleRefAndResourceRef
    private Long getAssignmentRefForRole(Long roleId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(roleId, resourceId);

        if (assignment.isPresent()) {
            //return Optional.of(assignment.get().getId());
            return assignment.get().getId();
        }
        return null;
    }
}
