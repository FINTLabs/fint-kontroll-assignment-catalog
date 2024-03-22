package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import no.fintlabs.resource.ResourceNotFoundException;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.user.UserRepository;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;

@Service
@Slf4j
public class AssignmentService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssigmentEntityProducerService assigmentEntityProducerService;
    private final ResourceRepository resourceRepository;

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private MembershipRepository membershipRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, AssigmentEntityProducerService assigmentEntityProducerService,
                             ResourceRepository resourceRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             FlattenedAssignmentRepository flattenedAssignmentRepository,
                             MembershipRepository membershipRepository) {
        this.assignmentRepository = assignmentRepository;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.membershipRepository = membershipRepository;
    }

    public Assignment createNewAssignment(Assignment assignment) {
        Long userRef = assignment.getUserRef();
        Long roleRef = assignment.getRoleRef();
        Long resourceRef = assignment.getResourceRef();

        assignment = handleUserAssignment(assignment, userRef, resourceRef);
        assignment = handleRoleAssignment(assignment, roleRef, resourceRef);
        assignment = handleResourceAssignment(assignment, resourceRef);

        log.info("Saving assignment {}", assignment.getAssignmentId());
        Assignment newAssignment = assignmentRepository.save(assignment);

        log.info("Creating flattened assignments for assignment with id {}", newAssignment.getAssignmentId());
        createFlattenedAssignments(newAssignment);

        //TODO: remove
        assigmentEntityProducerService.publish(newAssignment);

        return newAssignment;
    }

    public DetailedAssignment findAssignmentById(Long id) {

        return assignmentRepository
                .findById(id)
                .map(Assignment::toDetailedAssignment)
                .orElse(new DetailedAssignment());
    }

    public List<SimpleAssignment> getSimpleAssignments(
            FintJwtEndUserPrincipal principal
            //,List<String> orgUnits,
            //,String search
    ) {
        List<Assignment> assignments = assignmentRepository.findAll();

        return assignments
                .stream()
                .map(Assignment::toSimpleAssignment)
                .toList();
    }

    public void deleteAssignment(Long id) {
        Assignment assignment = assignmentRepository.getReferenceById(id);
        assignmentRepository.deleteById(id);
        assigmentEntityProducerService.publishDeletion(assignment);
    }

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Optional<Long> getAssignmentRefForUserAssignment(Long userId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId);
        return assignment.map(Assignment::getId);
    }

    public Optional<String> getAssignerUsernameForUserAssignment(Long userId, Long resourceId) {
        Optional<Assignment> userAssignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId);
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
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByRoleRefAndResourceRef(roleId, resourceId);
        return assignment.map(Assignment::getId);
    }

    public Optional<String> getAssignerUsernameForRoleAssignment(Long roleId, Long resourceId) {
        Optional<Assignment> roleAssignment = assignmentRepository.findAssignmentByRoleRefAndResourceRef(roleId, resourceId);
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
        return user.map(u -> u.getDisplayname());
    }

    private Assignment handleUserAssignment(Assignment assignment, Long userRef, Long resourceRef) {
        if (userRef != null) {
            if (existingUserAssignment(assignment)) {
                throw new AssignmentAlreadyExistsException(userRef.toString(), resourceRef.toString());
            }

            userRepository.findById(userRef).ifPresentOrElse(user -> {
                assignment.setAzureAdUserId(user.getIdentityProviderUserObjectId());
                assignment.setUserFirstName(user.getFirstName());
                assignment.setUserLastName(user.getLastName());
                assignment.setUserUserType(user.getUserType());
            }, () -> {
                throw new UserNotFoundException(userRef.toString());
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
                assignment.setAssignmentId(resourceRef + "_" + assignment.assignmentIdSuffix());
                assignment.setAzureAdGroupId(resource.getIdentityProviderGroupObjectId());
            }, () -> {
                throw new ResourceNotFoundException(resourceRef.toString());
            });
        }

        return assignment;
    }

    private void createFlattenedAssignments(Assignment assignment) {
        if (assignment.getUserRef() != null) {
            flattenedAssignmentRepository.save(toFlattenedAssignment(assignment));
        } else if (assignment.getRoleRef() != null) {
            List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

            if (memberships.isEmpty()) {
                log.info("Role (group) has no members. Saving flattened assignment without members. Roleref: {}", assignment.getRoleRef());
                flattenedAssignmentRepository.save(toFlattenedAssignment(assignment));
            } else {
                log.info("Saving flattened assignments for roleref {}", assignment.getRoleRef());
                memberships.forEach(membership -> flattenedAssignmentRepository.save(toFlattenedAssignment(assignment)));
            }
        }
    }

    private boolean existingRoleAssignment(Assignment assignment) {
        return assignmentRepository.findAssignmentByRoleRefAndResourceRef(assignment.getRoleRef(), assignment.getResourceRef()).isPresent();
    }

    private boolean existingUserAssignment(Assignment assignment) {
        return assignmentRepository.findAssignmentByUserRefAndResourceRef(assignment.getUserRef(), assignment.getResourceRef()).isPresent();
    }
}

