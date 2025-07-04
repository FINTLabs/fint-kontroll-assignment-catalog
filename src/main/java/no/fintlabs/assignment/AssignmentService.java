package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.ResourceNotFoundException;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssignmentService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResourceRepository resourceRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final ApplicationResourceLocationService applicationResourceLocationService;
    private final OpaService opaService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ResourceRepository resourceRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            FlattenedAssignmentService flattenedAssignmentService,
            ApplicationResourceLocationService applicationResourceLocationService,
            OpaService opaService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.applicationResourceLocationService = applicationResourceLocationService;
        this.opaService = opaService;
    }

    public Assignment createNewAssignment(Long resourceRef, String organizationUnitId, Long userRef, Long roleRef) {
        log.info("Trying to create new assignment for resource {} and {}", resourceRef, userRef != null ? "user " + userRef : "role " + roleRef);

        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(resourceRef)
                .organizationUnitId(organizationUnitId)
                .userRef(userRef)
                .roleRef(roleRef)
                .build();

        if (userRef != null) {
            handleDirectUserAssignment(assignment, userRef, resourceRef);
        }

        if (roleRef != null) {
            handleGroupAssignment(assignment, roleRef, resourceRef);
        }

        enrichByResource(assignment, resourceRef);

        log.info("Saving assignment {}", assignment);
        Assignment newAssignment = assignmentRepository.saveAndFlush(assignment);
        log.info("Saved assignment {}", newAssignment);

        flattenedAssignmentService.createFlattenedAssignments(newAssignment);
        log.info("Created flattened assignments for assignment id {}", newAssignment.getId());

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
        log.info("Deleting assignment with id {}", id);

        String userName = opaService.getUserNameAuthenticatedUser();

        Assignment assignment = assignmentRepository.getReferenceById(id);
        assignment.setAssignmentRemovedDate(new Date());

        userRepository.getUserByUserName(userName).ifPresent(user -> assignment.setAssignerRemoveRef(user.getId()));

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

    private void handleDirectUserAssignment(Assignment assignment, Long userRef, Long resourceRef) {
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

    private void handleGroupAssignment(Assignment assignment, Long roleRef, Long resourceRef) {
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

    private void enrichByResource(Assignment assignment, Long resourceRef) {
        resourceRepository.findById(resourceRef).ifPresentOrElse(resource -> {
            assignment.setResourceName(resource.getResourceName());
            assignment.setAssignmentId(resourceRef + "_" + assignment.assignmentIdSuffix() + "_" + LocalDateTime.now());
            assignment.setAzureAdGroupId(resource.getIdentityProviderGroupObjectId());

            Optional<NearestResourceLocationDto> nearestApplicationResourceLocationDto = applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(
                    resourceRef, assignment.getOrganizationUnitId());

            nearestApplicationResourceLocationDto.ifPresent(nearestApplicationResourceLocation -> {
                assignment.setApplicationResourceLocationOrgUnitId(nearestApplicationResourceLocation.orgUnitId());
                assignment.setApplicationResourceLocationOrgUnitName(nearestApplicationResourceLocation.orgUnitName());
            });
        }, () -> {
            throw new ResourceNotFoundException(resourceRef.toString());
        });
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

    public List<Assignment> getAssignmentsByRole(Long roleId) {
        return assignmentRepository.findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(roleId);
    }

    public List<Long> getAssignmentsByRoleAndUser(Long roleId, Long userId) {
        return assignmentRepository.findAssignmentIdsByRoleRefAndUserRefAndAssignmentRemovedDateIsNull(roleId, userId);
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    public List<Assignment> getActiveAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(userId);
    }

    public List<Assignment> getInactiveAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNotNull(userId);
    }

    public List<Assignment> getAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(userId);
    }

    public void deactivateAssignmentsByRole(Role role) {
        getAssignmentsByRole(role.getId())
                .forEach(assignment -> {
                    if (role.getRoleStatus().equalsIgnoreCase("inactive")) {
                        log.info("Deactivating assignment with id: {}", assignment.getId());
                        assignment.setAssignmentRemovedDate(new Date());
                        assignmentRepository.saveAndFlush(assignment);
                    }
                });
    }

    public void deactivateAssignmentsByUser(User user) {
        if (user.getStatus().equalsIgnoreCase("disabled")) {
            getActiveAssignmentsByUser(user.getId())
                    .forEach(assignment -> {
                        log.info("Deactivating assignment with id: {}", assignment.getId());
                        assignment.setAssignmentRemovedDate(new Date());
                        assignmentRepository.saveAndFlush(assignment);
                        flattenedAssignmentService.deleteFlattenedAssignments(assignment);
                    });
        }
    }

    @Async
    @Transactional
    public void updateAssignmentsWithApplicationResourceLocationOrgUnitAsync(Set<Long> ids) {
        updateAssignmentsWithApplicationResourceLocationOrgUnit(ids);
    }

    @Transactional
    public void updateAssignmentsWithApplicationResourceLocationOrgUnit(Set<Long> ids) {
        log.info("Updating {} assignments missing application resource location org unit", ids.size());

        List<Assignment> updatedAssignments = ids.stream()
                .map(assignmentRepository::findById)
                .flatMap(Optional::stream)
                .map(this::updateAssignmentWithNearestResource)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        if (!updatedAssignments.isEmpty()) {
            int batchSize = 800;

            for (int i = 0; i < updatedAssignments.size(); i += batchSize) {
                int end = Math.min(i + batchSize, updatedAssignments.size());
                List<Assignment> batch = updatedAssignments.subList(i, end);
                assignmentRepository.saveAll(batch);
            }
        }

        assignmentRepository.flush();

        log.info("Done updating. Updated {} of {} assignments missing application resource location org unit",
                 updatedAssignments.size(), ids.size());
    }

    private Optional<Assignment> updateAssignmentWithNearestResource(Assignment assignment) {
        return applicationResourceLocationService
                .getNearestApplicationResourceLocationForOrgUnit(
                        assignment.getResourceRef(),
                        assignment.getOrganizationUnitId())
                .map(nearest -> {
                    assignment.setApplicationResourceLocationOrgUnitId(nearest.orgUnitId());
                    assignment.setApplicationResourceLocationOrgUnitName(nearest.orgUnitName());
                    return assignment;
                });
    }
}

