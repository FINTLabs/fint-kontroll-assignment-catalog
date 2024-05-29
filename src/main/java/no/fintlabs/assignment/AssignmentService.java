package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.ResourceNotFoundException;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssignmentService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;

    private final ResourceRepository resourceRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;

    private final OpaService opaService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             ResourceRepository resourceRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             FlattenedAssignmentService flattenedAssignmentService,
                             OpaService opaService) {
        this.assignmentRepository = assignmentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.opaService = opaService;
    }

    public Assignment createNewAssignment(Assignment assignment) {
        Long userRef = assignment.getUserRef();
        Long roleRef = assignment.getRoleRef();
        Long resourceRef = assignment.getResourceRef();

        assignment = handleUserAssignment(assignment, userRef, resourceRef);
        log.info("Assignment after handling user assignment: {}", assignment);
        assignment = handleRoleAssignment(assignment, roleRef, resourceRef);
        log.info("Assignment after handling role assignment: {}", assignment);
        assignment = handleResourceAssignment(assignment, resourceRef);
        log.info("Assignment after handling resource assignment: {}", assignment);

        log.info("Saving assignment with id {}", assignment.getId());
        Assignment newAssignment = assignmentRepository.saveAndFlush(assignment);
        log.info("Saved assignment {}", newAssignment);

        flattenedAssignmentService.createFlattenedAssignments(newAssignment);
        log.info("Created flattened assignments for assignment {}", newAssignment);

        return newAssignment;
    }

    public List<SimpleAssignment> getSimpleAssignments() {
        Specification<Assignment> spec = AssignmentSpecificationBuilder.notDeleted();
        List<Assignment> assignments = assignmentRepository.findAll(spec);

        return assignments
                .stream()
                .map(Assignment::toSimpleAssignment)
                .toList();
    }

    public List<Assignment> getAssignments() {
        return assignmentRepository.findAll();
    }

    public Assignment deleteAssignment(Long id) {
        String userName = opaService.getUserNameAuthenticatedUser();

        User user = userRepository.getUserByUserName(userName)
                .orElseThrow(() -> new UserNotFoundException(userName));

        Assignment assignment = assignmentRepository.getReferenceById(id);
        assignment.setAssignmentRemovedDate(new Date());
        assignment.setAssignerRemoveRef(user.getId());
        Assignment assignmentForDeletion = assignmentRepository.saveAndFlush(assignment);

        flattenedAssignmentService.deleteFlattenedAssignments(assignment);

        return assignmentForDeletion;
    }

    public Optional<Long> getAssignmentRefForUserAssignment(Long userId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(userId, resourceId);
        return assignment.map(Assignment::getId);
    }

    public Optional<String> getAssignerUsernameForUserAssignment(Long userId, Long resourceId) {
        Optional<Assignment> userAssignment = assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(userId, resourceId);
        return userAssignment.map(Assignment::getAssignerUserName);
    }

    public Optional<String> getAssignerDisplaynameForUserAssignment(Long userId, Long resourceId) {
        Optional<String> username = getAssignerUsernameForUserAssignment(userId, resourceId);
        if (username.isPresent()) {
            return getDisplaynameFromUsername(username.get());
        }
        return Optional.empty();
    }

    public Optional<Long> getAssignmentRefForRoleAssignment(Long roleId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(roleId, resourceId);
        return assignment.map(Assignment::getId);
    }

    public Optional<String> getAssignerUsernameForRoleAssignment(Long roleId, Long resourceId) {
        Optional<Assignment> roleAssignment = assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(roleId, resourceId);
        return roleAssignment.map(Assignment::getAssignerUserName);
    }

    public Optional<String> getAssignerDisplaynameForRoleAssignment(Long roleId, Long resourceId) {
        Optional<String> username = getAssignerUsernameForRoleAssignment(roleId, resourceId);
        if (username.isPresent()) {
            return getDisplaynameFromUsername(username.get());
        }
        return Optional.empty();
    }

    private Optional<String> getDisplaynameFromUsername(String username) {
        Optional<User> user = userRepository.getUserByUserName(username);
        return user.map(User::getDisplayname);
    }

    private Assignment handleUserAssignment(Assignment assignment, Long userRef, Long resourceRef) {
        if (userRef != null) {
            if (existingUserFlattenedAssignmentNotTerminated(assignment)) {
                log.info("Assignment already exists for user {} and resource {}", userRef, resourceRef);
                throw new AssignmentAlreadyExistsException(userRef.toString(), resourceRef.toString());
            }

            userRepository.findById(userRef).ifPresentOrElse(user -> {
                assignment.setAzureAdUserId(user.getIdentityProviderUserObjectId());
                assignment.setUserFirstName(user.getFirstName());
                assignment.setUserLastName(user.getLastName());
                assignment.setUserUserType(user.getUserType());
            }, () -> {
                throw new UserNotFoundException(userRef);
            });
        }

        return assignment;
    }

    private Assignment handleRoleAssignment(Assignment assignment, Long roleRef, Long resourceRef) {
        if (roleRef != null) {
            if (existingRoleAssignment(assignment)) {
                throw new AssignmentAlreadyExistsException(roleRef.toString(), resourceRef.toString());
            }

            roleRepository.findById(roleRef).ifPresentOrElse(role -> {
                assignment.setRoleName(role.getRoleName());
                assignment.setRoleType(role.getRoleType());
            }, () -> {
                throw new RoleNotFoundException(roleRef.toString());
            });
        }

        return assignment;
    }

    private Assignment handleResourceAssignment(Assignment assignment, Long resourceRef) {
        if (resourceRef != null) {
            resourceRepository.findById(resourceRef).ifPresentOrElse(resource -> {
                assignment.setResourceName(resource.getResourceName());
                assignment.setAssignmentId(resourceRef + "_" + assignment.assignmentIdSuffix() + "_" + LocalDateTime.now());
                assignment.setAzureAdGroupId(resource.getIdentityProviderGroupObjectId());
            }, () -> {
                throw new ResourceNotFoundException(resourceRef.toString());
            });
        }

        return assignment;
    }

    private boolean existingUserFlattenedAssignmentNotTerminated(Assignment assignment) {
        return flattenedAssignmentService.getFlattenedAssignmentByUserAndResourceNotTerminated(assignment.getUserRef(), assignment.getResourceRef()).isPresent();
    }

    private boolean existingRoleAssignment(Assignment assignment) {
        return assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(assignment.getRoleRef(), assignment.getResourceRef()).isPresent();
    }

    private boolean existingUserAssignment(Assignment assignment) {
        return assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(assignment.getUserRef(), assignment.getResourceRef()).isPresent();
    }
}

