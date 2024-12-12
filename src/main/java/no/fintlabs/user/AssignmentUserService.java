package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.opa.OpaUtils;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

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

    public Page<ResourceAssignmentUser> findResourceAssignmentUsers(Long resourceId, String userType, List<String> orgUnits,
                                                                    List<String> orgUnitsInScope,
                                                                    String search,
                                                                    int page, int size) {

        List<String> orgUnitsToFilter = OpaUtils.getOrgUnitsToFilter(orgUnits, orgUnitsInScope);

        Pageable pageable = PageRequest.of(page, size);
//                                           ,Sort.by("u.firstName")
//                                                   .ascending()
//                                                   .and(Sort.by("u.lastName"))
//                                                   .ascending());

        log.info("Fetching flattenedassignments for resource with Id: " + resourceId);

        if(orgUnitsToFilter.contains(OrgUnitType.ALLORGUNITS.name())) {
            orgUnitsToFilter = null;
        }
        String fullName = null;
        String firstName = null;
        String lastName = null;

        if (search != null) {
            fullName = search.toLowerCase().strip().replaceAll("\\s+", ";");
            firstName = fullName.contains(";") ? StringUtils.substringBeforeLast(fullName, ";") : fullName;
            lastName = fullName.contains(";") ? StringUtils.substringAfterLast(fullName, ";") : fullName;
        }
        Page<Object[]> results = flattenedAssignmentRepository.findAssignmentsByResourceAndUserTypeAndNamesSearch(
                resourceId, userType, orgUnitsToFilter, firstName, lastName, fullName, pageable);

        return results.map(result -> {
            FlattenedAssignment flattenedAssignment = (FlattenedAssignment) result[0];
            User user = (User) result[1];
            Assignment assignment = (Assignment) result[2];
            Role role = (Role) result[3];
            String assignerFirstName = (String) result[4];
            String assignerLastName = (String) result[5];

            ResourceAssignmentUser resourceAssignmentUser = new ResourceAssignmentUser();
            resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
            resourceAssignmentUser.setAssignerUsername(assignment.getAssignerUserName());
            resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
            resourceAssignmentUser.setDirectAssignment(isDirectAssignment(flattenedAssignment));

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
            return resourceAssignmentUser;
        });
    }

    private boolean isDirectAssignment(FlattenedAssignment flattenedAssignment) {
        return flattenedAssignment.getAssignmentViaRoleRef() == null;
    }
}

