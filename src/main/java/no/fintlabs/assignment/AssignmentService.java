package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAssignmentValidator;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssignmentService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResourceAssignmentValidator resourceAssignmentValidator;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final ApplicationResourceLocationService applicationResourceLocationService;
    private final OpaService opaService;
    private final LicenseEnforcementService licenseEnforcementService;

    @Transactional
    public Assignment createNewAssignment(Long resourceRef, String organizationUnitId, Long userRef, Long roleRef) {
        log.info("Trying to create new assignment for resource {} and {}", resourceRef, userRef != null ? "user " + userRef : "role " + roleRef);

        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(resourceRef)
                .organizationUnitId(organizationUnitId)
                .userRef(userRef)
                .roleRef(roleRef)
                .build();

        Resource resource = resourceAssignmentValidator.getActiveResourceOrThrow(resourceRef);

        if (userRef != null) {
            handleDirectUserAssignment(assignment, userRef, resourceRef);
        }

        if (roleRef != null) {
            handleGroupAssignment(assignment, roleRef, resourceRef);
        }

        enrichByResource(assignment, resource);

        boolean availableCapacity = licenseEnforcementService.validateAssignmentCapacity(assignment);
        log.info("Validated license capacity for resource {} : {}",
                assignment.getResourceRef(), availableCapacity ? "Success" : "Failure");
        if (!availableCapacity) {
            throw new ConflictException("Can't assign more licenses for resource " + assignment.getResourceRef());
        }

        log.info("Saving assignment {}", assignment);
        Assignment newAssignment = assignmentRepository.saveAndFlush(assignment);
        log.info("Saved assignment {}", newAssignment);

        flattenedAssignmentService.createFlattenedAssignmentsSync(newAssignment);
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

    public List<Assignment> getAllUserAssignments() {
        return assignmentRepository.findAllByUserRefIsNotNullOrRoleRefIsNotNull();
    }

    public List<Assignment> getAllActiveUserAssignments() {
        return assignmentRepository.findAllActiveUserAssignments();
    }

    @Transactional
    public Assignment deleteAssignment(Long id) {
        log.info("Deleting assignment with id {}", id);

        String userName = opaService.getUserNameAuthenticatedUser();

        Assignment assignment = assignmentRepository.getReferenceById(id);
        assignment.setAssignmentRemovedDate(new Date());

        if (!userName.isEmpty()) {
            userRepository.getUserByUserName(userName).ifPresent(user -> assignment.setAssignerRemoveRef(user.getId()));
        }


        Assignment assignmentForDeletion = assignmentRepository.saveAndFlush(assignment);

        flattenedAssignmentService.deleteFlattenedAssignments(assignment, "Associated assignment removed by user");

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
            return getDisplayNameFromUsername(username.get());
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
            return getDisplayNameFromUsername(username.get());
        }
        return Optional.empty();
    }

    private Optional<String> getDisplayNameFromUsername(String username) {
        Optional<User> user = userRepository.getUserByUserName(username);
        return user.map(User::getDisplayname);
    }

    private void handleDirectUserAssignment(Assignment assignment, Long userRef, Long resourceRef) {
        if (existingDirectUserAssignmentNotTerminated(assignment)) {
            log.warn("Active assignment already exists for user {} and resource {}", userRef, resourceRef);
            throw new ConflictException("Active assignment already exists for user " + userRef + " and resource " + resourceRef);
        }

        userRepository.findById(userRef).ifPresentOrElse(user -> {
            if (user.getIdentityProviderUserObjectId() == null) {
                throw new AssignmentException(HttpStatus.UNPROCESSABLE_ENTITY, "User " + userRef + " does not have entra user id set");
            }
            assignment.setEntraUserId(user.getIdentityProviderUserObjectId());
            assignment.setUserFirstName(user.getFirstName());
            assignment.setUserLastName(user.getLastName());
            assignment.setUserUserType(user.getUserType());
        }, () -> {
            throw new ResourceNotFoundException("User not found: " + userRef);
        });
    }

    private void handleGroupAssignment(Assignment assignment, Long roleRef, Long resourceRef) {
        if (existingRoleAssignment(assignment)) {
            log.warn("Active assignment already exists for role {} and resource {}", roleRef, resourceRef);
            throw new ConflictException("Active assignment already exists for role " + roleRef + " and resource " + resourceRef);
        }

        roleRepository.findById(roleRef).ifPresentOrElse(role -> {
            assignment.setRoleName(role.getRoleName());
            assignment.setRoleType(role.getRoleType());
        }, () -> {
            throw new RoleNotFoundException(roleRef.toString());
        });
    }

    private void enrichByResource(Assignment assignment, Resource resource) {
        assignment.setResourceName(resource.getResourceName());
        assignment.setAssignmentId(resource.getId() + "_" + assignment.assignmentIdSuffix() + "_" + LocalDateTime.now());
        assignment.setEntraGroupId(resource.getIdentityProviderGroupObjectId());

        Optional<NearestResourceLocationDto> nearestApplicationResourceLocationDto = applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(
                resource.getId(), assignment.getOrganizationUnitId());

        nearestApplicationResourceLocationDto.ifPresent(nearestApplicationResourceLocation -> {
            assignment.setApplicationResourceLocationOrgUnitId(nearestApplicationResourceLocation.orgUnitId());
            assignment.setApplicationResourceLocationOrgUnitName(nearestApplicationResourceLocation.orgUnitName());
        });
    }

    private boolean existingDirectUserAssignmentNotTerminated(Assignment assignment) {
        return assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(
                assignment.getUserRef(),
                assignment.getResourceRef()
        ).isPresent();
    }

    private boolean existingRoleAssignment(Assignment assignment) {
        return assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(assignment.getRoleRef(), assignment.getResourceRef()).isPresent();
    }

    public List<Assignment> getAssignmentsByRole(Long roleId) {
        return assignmentRepository.findAssignmentsByRoleRefAndAssignmentRemovedDateIsNull(roleId);
    }

    public List<Assignment> getAssignmentsByRoles(List<Long> roleIds) {
        return roleIds.isEmpty()
                ? List.of()
                : assignmentRepository.findAssignmentsByRoleRefInAndAssignmentRemovedDateIsNull(roleIds);
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(id);
    }

    public List<Assignment> getActiveAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(userId);
    }
    public List<Assignment> getActiveUserAssignmentsByResource(Long resourceId) {
        return assignmentRepository.findActiveUserAssignmentsByResourceRef(resourceId);
    }

    public List<Assignment> getInactiveAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNotNull(userId);
    }

    public List<Assignment> getAssignmentsByUser(Long userId) {
        return assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(userId);
    }

    public void deactivateAssignmentsByRole(Role role) {
        getAssignmentsByRole(role.getId())
                .forEach(assignment -> deactivateAssignment(assignment, "Role is no longer active"));
    }

    public void deactivateAssignmentsByUserId(Long userId) {
        getActiveAssignmentsByUser(userId)
                .forEach(assignment -> deactivateAssignment(assignment, "User is no longer active"));
    }

    public void deactivateAssignmentsByResourceId(Long resourceId) {
        getActiveUserAssignmentsByResource(resourceId)
                .forEach(assignment -> deactivateAssignment(assignment, "Resource is no longer active"));
    }

    private void deactivateAssignment(Assignment assignment, String terminationReason) {
        log.info("Deactivating assignment with id: {}", assignment.getId());
        assignment.setAssignmentRemovedDate(new Date());
        assignmentRepository.saveAndFlush(assignment);
        flattenedAssignmentService.deleteFlattenedAssignments(assignment, terminationReason);
        log.info("Updated license count after deactivating assignment {} : {}",
                assignment.getId(), licenseEnforcementService.recalculateAssignedResources(assignment) ? "Success" : "Failure");
    }

    @Transactional
    public void updateAssignmentsWithApplicationResourceLocationOrgUnit(Set<Long> ids) {
        log.info("Updating application resource location org unit for {} assignments", ids.size());

        List<Assignment> updatedAssignments = assignmentRepository.findAllById(ids).stream()
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
        updatedAssignments.forEach(assignment ->
                flattenedAssignmentService.syncFlattenedAssignmentsSynchronously(assignment, false));

        log.info("Done updating application resource location org unit. Updated {} of {} assignments",
                updatedAssignments.size(), ids.size());
    }

    private Optional<Assignment> updateAssignmentWithNearestResource(Assignment assignment) {
        Optional<NearestResourceLocationDto> nearestLocation = applicationResourceLocationService
                .getNearestApplicationResourceLocationForOrgUnit(
                        assignment.getResourceRef(),
                        assignment.getOrganizationUnitId());

        nearestLocation.ifPresentOrElse(nearest -> {
            assignment.setApplicationResourceLocationOrgUnitId(nearest.orgUnitId());
            assignment.setApplicationResourceLocationOrgUnitName(nearest.orgUnitName());
            assignment.setIsInvalid(false);
        }, () -> {
            assignment.setApplicationResourceLocationOrgUnitId(null);
            assignment.setApplicationResourceLocationOrgUnitName(null);
            assignment.setIsInvalid(true);
        });
        return Optional.of(assignment);
    }

    public void updateAllAssignmentsOnUserChange(User user) {
        List<Assignment> assignments = getAssignmentsByUser(user.getId());
        assignments.forEach(assignment -> assignment.setEntraUserId(user.getIdentityProviderUserObjectId()));
        assignmentRepository.saveAll(assignments);
        assignmentRepository.flush();
        log.info("Updated {} assignments for user {}", assignments.size(), user.getId());
        flattenedAssignmentService.updateAssignmentsOnUserChange(user);
    }

    @Transactional
    public void reassignLinkedAssignments(Long resourceRef, String orgUnitId) {
        List<Assignment> existingAssignments = assignmentRepository.findAssignmentsByResourceRefAndApplicationResourceLocationOrgUnitIdAndAssignmentRemovedDateIsNull(resourceRef, orgUnitId);
        log.info("Found {} assignments for resource {} and orgUnitId {}", existingAssignments.size(), resourceRef, orgUnitId);
        List<Assignment> updatedAssignments = new ArrayList<>();
        existingAssignments.forEach(assignment -> {
            updateAssignmentWithNearestResource(assignment).ifPresentOrElse(updatedAssignments::add, () -> {
                log.info("Assignment {} is invalid and will not be reassigned", assignment.getId());
                assignment.setIsInvalid(true);
                updatedAssignments.add(assignment);
            });
        });
        assignmentRepository.saveAll(updatedAssignments);
        assignmentRepository.flush();
        updatedAssignments.forEach(this::syncFlattenedAssignmentLocation);
        log.info("Done reassigning {} assignments for resource {} and orgUnitId {}", updatedAssignments.size(), resourceRef, orgUnitId);
    }

    private void syncFlattenedAssignmentLocation(Assignment assignment) {
        if (assignment.getDeviceGroupRef() != null) {
            flattenedDeviceAssignmentService.updateApplicationResourceLocation(assignment);
            return;
        }
        flattenedAssignmentService.syncFlattenedAssignmentsSynchronously(assignment, false);
    }
}
