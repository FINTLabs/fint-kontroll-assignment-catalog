package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.role.Role;
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
public class AssigmentUserService {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;


    public AssigmentUserService(UserRepository userRepository, AssignmentService assignmentService, FlattenedAssignmentRepository flattenedAssignmentRepository) {
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

        UserSpecificationBuilder userSpecificationBuilder =
                new UserSpecificationBuilder(resourceId, userType, orgUnits, orgUnitsInScope, search);
        Specification<FlattenedAssignment> userSpecification = userSpecificationBuilder.flattenedAssignmentSearch();

        Pageable pageable = PageRequest.of(page, size,
                                           Sort.by("user.firstName")
                                                   .ascending()
                                                   .and(Sort.by("user.lastName"))
                                                   .ascending());

        log.info("Fetching flattenedassignments for resource with Id: " + resourceId);

        return flattenedAssignmentRepository.findAll(userSpecification, pageable)
                .map(flattenedAssignment -> {
                    ResourceAssignmentUser resourceAssignmentUser = new ResourceAssignmentUser();
                    resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
                    resourceAssignmentUser.setAssignerUsername(flattenedAssignment.getAssignment().getAssignerUserName());
                    resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
                    resourceAssignmentUser.setDirectAssignment(isDirectAssignment(flattenedAssignment));

                    if (flattenedAssignment.getUser() != null) {
                        User user = flattenedAssignment.getUser();
                        resourceAssignmentUser.setAssigneeUsername(user.getUserName());
                        resourceAssignmentUser.setAssigneeRef(user.getId());
                        resourceAssignmentUser.setAssigneeUserType(user.getUserType());
                        resourceAssignmentUser.setAssigneeOrganisationUnitId(user.getOrganisationUnitId());
                        resourceAssignmentUser.setAssigneeOrganisationUnitName(user.getOrganisationUnitName());
                        resourceAssignmentUser.setAssigneeFirstName(user.getFirstName());
                        resourceAssignmentUser.setAssigneeLastName(user.getLastName());
                    }

                    if(flattenedAssignment.getRole() != null) {
                        Role role = flattenedAssignment.getRole();
                        resourceAssignmentUser.setAssignmentViaRoleName(role.getRoleName());
                    }

                    Optional<User> assignerUser =
                            userRepository.getUserByUserName(flattenedAssignment.getAssignment().getAssignerUserName());
                    Optional<String> assignerDisplayName = assignerUser.map(User::getDisplayname);

                    resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName.orElse(null));

                    if(resourceAssignmentUser.getAssigneeFirstName() == null && resourceAssignmentUser.getAssigneeLastName() == null) {
                        resourceAssignmentUser.setAssigneeFirstName(flattenedAssignment.getAssignment().getUserFirstName());
                        resourceAssignmentUser.setAssigneeLastName(flattenedAssignment.getAssignment().getUserLastName());

                    }
                    return resourceAssignmentUser;
                });
    }

    private boolean isDirectAssignment(FlattenedAssignment flattenedAssignment) {
        return flattenedAssignment.getAssignmentViaRoleRef() == null;
    }
}

