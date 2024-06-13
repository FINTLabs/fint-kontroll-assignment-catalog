package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssignmentResourceService {

    private final ResourceRepository resourceRepository;
    private final AssignmentService assignmentService;

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    private final UserRepository userRepository;

    public AssignmentResourceService(ResourceRepository resourceRepository, AssignmentService assignmentService, FlattenedAssignmentRepository flattenedAssignmentRepository,
                                     UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.assignmentService = assignmentService;
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.userRepository = userRepository;
    }

    public Page<AssignmentResource> getResourcesAssignedToUser(Long userId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec, page)
                .map(Resource::toSimpleResource)
                .map(resource -> {
                    assignmentService.getAssignmentRefForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignmentRef);
                    assignmentService.getAssignerUsernameForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForUserAssignment(userId, resource.getId()).ifPresent(resource::setAssignerDisplayname);
                    return resource;
                });
        return resources;
    }

    public Page<AssignmentResource> getResourcesAssignedToRole(Long roleId, Specification<Resource> spec, Pageable page) {
        Page<AssignmentResource> resources = resourceRepository.findAll(spec, page)
                .map(Resource::toSimpleResource)
                .map(resource -> {
                    assignmentService.getAssignmentRefForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignmentRef);
                    assignmentService.getAssignerUsernameForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForRoleAssignment(roleId, resource.getId()).ifPresent(resource::setAssignerDisplayname);
                    return resource;
                });
        return resources;
    }

    public Page<UserAssignmentResource> findUserAssignmentResourcesByRole(Long roleId, String resourceType, List<String> orgUnits,
                                                                          List<String> orgUnitsInScope, String search, int page, int size) {
        ResourceSpecificationBuilder resourceSpecificationBuilder =
                new ResourceSpecificationBuilder(null, roleId, resourceType, orgUnits, orgUnitsInScope, search);

        Specification<FlattenedAssignment> spec = resourceSpecificationBuilder.flattenedAssignmentSearchByRole();

        Pageable pageable = PageRequest.of(page, size,
                                           Sort.by("user.firstName")
                                                   .ascending()
                                                   .and(Sort.by("user.lastName"))
                                                   .ascending());

        log.info("Fetching flattenedassignments for role with Id: " + roleId);

        return flattenedAssignmentRepository.findAll(spec, pageable)
                .map(flattenedAssignment -> {
                    UserAssignmentResource resourceAssignmentUser = new UserAssignmentResource();
                    resourceAssignmentUser.setResourceRef(flattenedAssignment.getResourceRef());
                    resourceAssignmentUser.setResourceName(flattenedAssignment.getResource().getResourceName());
                    resourceAssignmentUser.setResourceType(flattenedAssignment.getResource().getResourceType());
                    resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
                    resourceAssignmentUser.setDirectAssignment(isDirectAssignment(flattenedAssignment));
                    resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
                    resourceAssignmentUser.setAssignerUsername(flattenedAssignment.getAssignment().getAssignerUserName());

                    if (flattenedAssignment.getRole() != null) {
                        Role role = flattenedAssignment.getRole();
                        resourceAssignmentUser.setAssignmentViaRoleName(role.getRoleName());
                    }

                    Optional<User> assignerUser =
                            userRepository.getUserByUserName(flattenedAssignment.getAssignment().getAssignerUserName());
                    Optional<String> assignerDisplayName = assignerUser.map(User::getDisplayname);

                    resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName.orElse(null));

                    return resourceAssignmentUser;
                });
    }

    public Page<UserAssignmentResource> findUserAssignmentResourcesByUser(Long userId, String resourceType, List<String> orgUnits,
                                                                          List<String> orgUnitsInScope, String search, int page, int size) {
        ResourceSpecificationBuilder resourceSpecificationBuilder =
                new ResourceSpecificationBuilder(userId, null, resourceType, orgUnits, orgUnitsInScope, search);

        Specification<FlattenedAssignment> spec = resourceSpecificationBuilder.flattenedAssignmentSearchByUser();

        Pageable pageable = PageRequest.of(page, size,
                                           Sort.by("user.firstName")
                                                   .ascending()
                                                   .and(Sort.by("user.lastName"))
                                                   .ascending());

        log.info("Fetching flattenedassignments for user with Id: " + userId);

        return flattenedAssignmentRepository.findAll(spec, pageable)
                .map(flattenedAssignment -> {
                    UserAssignmentResource resourceAssignmentUser = new UserAssignmentResource();
                    resourceAssignmentUser.setResourceRef(flattenedAssignment.getResourceRef());
                    resourceAssignmentUser.setResourceName(flattenedAssignment.getResource().getResourceName());
                    resourceAssignmentUser.setResourceType(flattenedAssignment.getResource().getResourceType());
                    resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
                    resourceAssignmentUser.setDirectAssignment(isDirectAssignment(flattenedAssignment));
                    resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
                    resourceAssignmentUser.setAssignerUsername(flattenedAssignment.getAssignment().getAssignerUserName());

                    if (flattenedAssignment.getRole() != null) {
                        Role role = flattenedAssignment.getRole();
                        resourceAssignmentUser.setAssignmentViaRoleName(role.getRoleName());
                    }

                    Optional<User> assignerUser =
                            userRepository.getUserByUserName(flattenedAssignment.getAssignment().getAssignerUserName());
                    Optional<String> assignerDisplayName = assignerUser.map(User::getDisplayname);

                    resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName.orElse(null));

                    return resourceAssignmentUser;
                });
    }

    private boolean isDirectAssignment(FlattenedAssignment flattenedAssignment) {
        return flattenedAssignment.getAssignmentViaRoleRef() == null;
    }
}
