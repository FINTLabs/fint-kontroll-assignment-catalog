package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.authorization.AuthorizationUtil;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AssignmentResourceService {

    private final ResourceRepository resourceRepository;
    private final AssignmentService assignmentService;

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final AuthorizationUtil authorizationUtil;

    private final UserRepository userRepository;

    public AssignmentResourceService(
            ResourceRepository resourceRepository,
            AssignmentService assignmentService,
            FlattenedAssignmentRepository flattenedAssignmentRepository,
            AuthorizationUtil authorizationUtil,
            UserRepository userRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.assignmentService = assignmentService;
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.authorizationUtil = authorizationUtil;
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
                                                                          List<String> orgUnitsInScope, List<Long> resourceIds, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("Fetching flattened assignments for role with Id: " + roleId);

        Page<Object[]> results = flattenedAssignmentRepository.findAssignmentsByRoleAndResourceTypeAndSearch(roleId, resourceType, resourceIds, search, pageable);

        return results.map(this::mapToUserAssignmentResource);
    }

    public Page<UserAssignmentResource> findUserAssignmentResourcesByUser(Long userId, String resourceType, List<String> orgUnits,
                                                                          List<String> orgUnitsInScope, List<Long> resourceIds, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        log.info("Fetching flattened assignments for user with Id: " + userId);

        Page<Object[]> results = flattenedAssignmentRepository.findAssignmentsByUserAndResourceTypeAndSearch(userId, resourceType, resourceIds, search, pageable);

        return results.map(this::mapToUserAssignmentResource);
    }

    private UserAssignmentResource mapToUserAssignmentResource(Object[] result) {
        FlattenedAssignment flattenedAssignment = (FlattenedAssignment) result[0];
        Resource resource = (Resource) result[1];
        Role role = (Role) result[2];
        User user = (User) result[3];
        Assignment assignment = (Assignment) result[4];
        String assignerFirstName = (String) result[5];
        String assignerLastName = (String) result[6];
        String objectType = (String) result[7];

        UserAssignmentResource resourceAssignmentUser = new UserAssignmentResource();
        resourceAssignmentUser.setResourceRef(flattenedAssignment.getResourceRef());
        resourceAssignmentUser.setResourceName(resource.getResourceName());
        resourceAssignmentUser.setResourceType(resource.getResourceType());
        resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
        resourceAssignmentUser.setDirectAssignment(isDirectAssignment(assignment));
        resourceAssignmentUser.setDeletableAssignment(isDeletableAssignment(assignment,resource, objectType));
        resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
        resourceAssignmentUser.setAssignerUsername(assignment.getAssignerUserName());

        if (user != null) {
            resourceAssignmentUser.setAssigneeRef(user.getId());
        }
        if (role != null) {
            resourceAssignmentUser.setAssignmentViaRoleName(role.getRoleName());
        }
        String assignerDisplayName = (assignerFirstName != null && assignerLastName != null) ? assignerFirstName + " " + assignerLastName : null;
        resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName);

        return resourceAssignmentUser;
    }

    private boolean isDirectAssignment(Assignment assignment) {
        return assignment.isUserAssignment();
    }
    private boolean isDeletableAssignment(Assignment assignment, Resource resource, String objectType) {
        List<String> orgUnitsInScope = authorizationUtil.getAllAuthorizedOrgUnitIDs();
        return ((objectType.equals("user") && isDirectAssignment(assignment) || objectType.equals("role"))
                && (assignment.getApplicationResourceLocationOrgUnitId() != null && orgUnitsInScope.contains(assignment.getApplicationResourceLocationOrgUnitId())
                || isResourceUnrestricted(resource)));
    }
    private boolean isResourceUnrestricted(Resource resource) {
        //TODO: temporary solution, should be replaced with a proper check
        if (resource.getLicenseEnforcement() == null) {
            return false;
        }
        List<String> unrestrictedEnforcementTypes = List.of(
                Handhevingstype.NOTSPECIFIED.name(),
                Handhevingstype.NOTSET.name(),
                Handhevingstype.FREEALL.name(),
                Handhevingstype.FREEEDU.name(),
                Handhevingstype.FREESTUDENT.name());
        return unrestrictedEnforcementTypes.contains(resource.getLicenseEnforcement());
    }
}
