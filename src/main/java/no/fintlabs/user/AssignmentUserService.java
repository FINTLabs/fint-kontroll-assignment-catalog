package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.kodeverk.ScopeType;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.opa.OpaUtils;
import no.fintlabs.resource.Resource;
import no.fintlabs.role.Role;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssignmentUserService {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    public AssignmentUserService(UserRepository userRepository, AssignmentService assignmentService, FlattenedAssignmentRepository flattenedAssignmentRepository) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
    }

    public Page<AssignmentUser> findBySearchCriteria(Long resourceId, Specification<User> spec, Pageable page) {
        return userRepository.findAll(spec, page)
                .map(User::toAssignmentUser)
                .map(user -> {
                    assignmentService.getAssignmentRefForUserAssignment(user.getId(), resourceId).ifPresent(user::setAssignmentRef);
                    assignmentService.getAssignerUsernameForUserAssignment(user.getId(), resourceId).ifPresent(user::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForUserAssignment(user.getId(), resourceId)
                            .ifPresent(user::setAssignerDisplayname);
                    return user;
                });
    }

    public Page<ResourceAssignmentUser> findResourceAssignmentUsersForResourceId(
            Long resourceId,
            String userType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            List<Long> userIds,
            String search,
            int page,
            int size
    ) {
        List<String> orgUnitsToFilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Pageable pageable = PageRequest.of(page, size);

        log.info("Fetching flattened assignments for resource with Id: {}",  resourceId);

        if(orgUnitsToFilter.contains("ALLORGUNITS")) {
            orgUnitsToFilter = null;
        }
        String fullName = null;
        String firstName = null;
        String lastName = null;

        if (search != null) {
            fullName = search.toLowerCase().strip().replaceAll("\\s+", "%");
            firstName = fullName.contains("%") ? StringUtils.substringBeforeLast(fullName, "%") : null;
            lastName = fullName.contains("%") ? StringUtils.substringAfterLast(fullName, "%") : fullName;
        }

        Optional< Page<Object[]>> results = flattenedAssignmentRepository.findAssignmentsByResourceAndUserTypeAndNamesSearch(
                resourceId, userType, orgUnitsToFilter, firstName, lastName, fullName, userIds, pageable);

        if (results.isEmpty()) {
            log.warn("Fetching flattened assignments for resource with Id: {} returned no results",  resourceId);
            return null;
        }
        log.info("Fetching flattened assignments for resource with Id: {} returned {} objects",  resourceId, results.get().getSize());

        return results.get().map(result -> {
            FlattenedAssignment flattenedAssignment = (FlattenedAssignment) result[0];
            Resource resource = (Resource) result[1];
            User user = (User) result[2];
            Assignment assignment = (Assignment) result[3];
            Role role = (Role) result[4];
            String assignerFirstName = (String) result[5];
            String assignerLastName = (String) result[6];

            boolean isDirectAssignment = isDirectAssignment(flattenedAssignment);
            boolean isDeletableAssignment = isDirectAssignment &&
                    (isAllOrgUnitsInScope(orgUnitsInScope) ||
                            isResourceUnrestricted(resource) ||
                            isResourceLocationInScope(assignment, orgUnitsInScope)
                    );

            ResourceAssignmentUser resourceAssignmentUser = new ResourceAssignmentUser();
            resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
            resourceAssignmentUser.setAssignerUsername(assignment.getAssignerUserName());
            resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
            resourceAssignmentUser.setDirectAssignment(isDirectAssignment);
            resourceAssignmentUser.setDeletableAssignment(isDeletableAssignment);

            if (user != null) {
                resourceAssignmentUser.setAssigneeUsername(user.getUserName());
                resourceAssignmentUser.setAssigneeRef(user.getId());
                resourceAssignmentUser.setAssigneeUserType(user.getUserType());
                resourceAssignmentUser.setAssigneeOrganisationUnitId(user.getOrganisationUnitId());
                resourceAssignmentUser.setAssigneeOrganisationUnitName(user.getOrganisationUnitName());
                resourceAssignmentUser.setAssigneeFirstName(user.getFirstName());
                resourceAssignmentUser.setAssigneeLastName(user.getLastName());
            }

            if (role != null) {
                resourceAssignmentUser.setAssignmentViaRoleName(role.getRoleName());
            }

            String assignerDisplayName = (assignerFirstName != null && assignerLastName != null) ? assignerFirstName + " " + assignerLastName : null;
            resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName);

            if (resourceAssignmentUser.getAssigneeFirstName() == null && resourceAssignmentUser.getAssigneeLastName() == null) {
                resourceAssignmentUser.setAssigneeFirstName(assignment.getUserFirstName());
                resourceAssignmentUser.setAssigneeLastName(assignment.getUserLastName());
            }
            log.info("Returning resource assignment user from flattened assignement {} - resource id: {} assignment id: {} user id: {} is direct: {} via role id: {} is deletable: {} ",
                    flattenedAssignment.getAssignmentId(),
                    resource.getId(),
                    resourceAssignmentUser.getAssignmentRef(),
                    resourceAssignmentUser.getAssigneeRef(),
                    resourceAssignmentUser.isDirectAssignment(),
                    resourceAssignmentUser.getAssignmentViaRoleRef(),
                    resourceAssignmentUser.isDeletableAssignment()

                    );
            return resourceAssignmentUser;
        });
    }

    private boolean isDirectAssignment(FlattenedAssignment flattenedAssignment) {
        return flattenedAssignment.getAssignmentViaRoleRef() == null;
    }

    private boolean isResourceLocationInScope(Assignment assignment,  List<String> orgUnitsInScope) {
        try {
            boolean isApplicationResourceLocationInScope =
                    assignment.getApplicationResourceLocationOrgUnitId() != null &&
                    orgUnitsInScope.contains(assignment.getApplicationResourceLocationOrgUnitId());
            log.info("Resource location {} in scope for assignment {} is: {}",
                    assignment.getApplicationResourceLocationOrgUnitId(),
                    assignment.getId(),
                    isApplicationResourceLocationInScope);

            return isApplicationResourceLocationInScope;
        }
        catch (Exception e)
        {
           log.error("Calculation of isResourceLocationInScope for assignment {} failed with error {}",
                   assignment.getId(),
                   e.getMessage());
           return false;
        }
    }

    private boolean isResourceUnrestricted(Resource resource) {
        //TODO: temporary solution, should be replaced with a proper check
        if (resource.getLicenseEnforcement() == null) {
            return true;
        }
        List<String> unrestrictedEnforcementTypes = List.of(
                Handhevingstype.NOTSPECIFIED.name(),
                Handhevingstype.NOTSET.name(),
                Handhevingstype.FREEALL.name(),
                Handhevingstype.FREEEDU.name(),
                Handhevingstype.FREESTUDENT.name());
        boolean isResourceUnrestricted = unrestrictedEnforcementTypes.contains(resource.getLicenseEnforcement());
        log.info("Resource {} is unrestricted: {}", resource.getId(), isResourceUnrestricted);
        return isResourceUnrestricted;
    }
    private boolean isAllOrgUnitsInScope(List<String> orgUnitsInScope) {
        boolean isAllOrgUnitsInScope = orgUnitsInScope.stream()
                .anyMatch(ScopeType.ALLORGUNITS.name()::equals);
        log.info("Scope contains {}: {}", ScopeType.ALLORGUNITS.name(), isAllOrgUnitsInScope);
        return isAllOrgUnitsInScope;
    }
}

