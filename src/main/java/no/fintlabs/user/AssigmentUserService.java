package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
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

    public AssigmentUserService(UserRepository userRepository, AssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
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
        Specification<User> userSpecification = userSpecificationBuilder.flattenedAssignmentSearch();

        Pageable pageable = PageRequest.of(page, size,
                                           Sort.by("firstName")
                                                   .ascending()
                                                   .and(Sort.by("lastName"))
                                                   .ascending());

        log.info("Fetching users for resource with Id: " + resourceId);

        return userRepository.findAll(userSpecification, pageable)
                .map(user -> {
                    ResourceAssignmentUser resourceAssignmentUser = new ResourceAssignmentUser();
                    resourceAssignmentUser.setAssigneeRef(user.getId());
                    resourceAssignmentUser.setAssigneeFirstName(user.getFirstName());
                    resourceAssignmentUser.setAssigneeLastName(user.getLastName());
                    resourceAssignmentUser.setAssigneeUsername(user.getUserName());
                    resourceAssignmentUser.setAssigneeUserType(user.getUserType());
                    resourceAssignmentUser.setAssigneeOrganisationUnitId(user.getOrganisationUnitId());
                    resourceAssignmentUser.setAssigneeOrganisationUnitName(user.getOrganisationUnitName());

                    user.getFlattenedAssignments().stream()
                            .findFirst()
                            .ifPresent(flattenedAssignment -> {
                                resourceAssignmentUser.setAssignmentRef(flattenedAssignment.getAssignmentId());
                                resourceAssignmentUser.setAssignerUsername(flattenedAssignment.getAssignment().getAssignerUserName());

                                Optional<User> assignerUser =
                                        userRepository.getUserByUserName(flattenedAssignment.getAssignment().getAssignerUserName());
                                Optional<String> assignerDisplayName = assignerUser.map(User::getDisplayname);

                                resourceAssignmentUser.setAssignerDisplayname(assignerDisplayName.orElse(null));
                                resourceAssignmentUser.setAssignmentViaRoleRef(flattenedAssignment.getAssignmentViaRoleRef());
                                resourceAssignmentUser.setAssignmentViaRoleName(
                                        flattenedAssignment.getRole() != null ? flattenedAssignment.getRole().getRoleName() : null);
                                resourceAssignmentUser.setDirectAssignment(isDirectAssignment(flattenedAssignment));
                            });

                    return resourceAssignmentUser;
                });
    }

    private boolean isDirectAssignment(FlattenedAssignment flattenedAssignment) {
        return flattenedAssignment.getAssignmentViaRoleRef() == null;
    }
}

