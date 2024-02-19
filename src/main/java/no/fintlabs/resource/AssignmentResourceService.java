package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AssignmentResourceService {
    private final ResourceRepository resourceRepository;
    private final AssignmentService assignmentService;

    public AssignmentResourceService(ResourceRepository resourceRepository, AssignmentService assignmentService) {
        this.resourceRepository = resourceRepository;
        this.assignmentService = assignmentService;
    }

    public Page<AssignmentResource> getResourcesAssignedToUser(Long userId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec,page)
                .map(Resource::toSimpleResource)
                .map(resource ->  {
                    assignmentService.getAssignmentRefForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignmentRef);
                    assignmentService.getAssignerUsernameForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignerDisplayname);
                    return resource;
                });
        return resources;
    }
    public Page<AssignmentResource> getResourcesAssignedToRole(Long roleId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec,page)
                .map(Resource::toSimpleResource)
                .map(resource ->  {
                    assignmentService.getAssignmentRefForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignmentRef);
                    assignmentService.getAssignerUsernameForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignerDisplayname);
                    return resource;
                });
        return resources;
    }

}
