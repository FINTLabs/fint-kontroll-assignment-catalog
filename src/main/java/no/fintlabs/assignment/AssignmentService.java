package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssignmentService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SimpeAssignmentService simpeAssignmentService;
    private final ResourceRepository resourceRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, SimpeAssignmentService simpeAssignmentService, ResourceRepository resourceRepository,
                             UserRepository userRepository,
                             RoleRepository roleRepository) {
        this.assignmentRepository = assignmentRepository;
        this.simpeAssignmentService = simpeAssignmentService;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public Assignment createNewAssignment(Assignment assignment) {
        //TODO: handle Optional.Empty return
        //TODO: Handle both roleRef and userRef null
        Long userRef = assignment.getUserRef();
        Long roleRef = assignment.getRoleRef();
        Long resourceRef = assignment.getResourceRef();

        if (userRef != null) {
            Optional<Assignment> existingUserAssignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userRef, resourceRef);

            if (existingUserAssignment.isPresent()) {
                throw new AssignmentAlreadyExistsException(userRef.toString(), resourceRef.toString());
            }
            User user = userRepository.findById(userRef).get();
            assignment.setAzureAdUserId(user.getAzureAdUserId());
            assignment.setUserFirstName(user.getFirstName());
            assignment.setUserLastName(user.getLastName());
            assignment.setUserUserType(user.getUserType());
        }

        if (roleRef != null) {
            Optional<Assignment> existingRoleAssignment = assignmentRepository.findAssignmentByRoleRefAndResourceRef(roleRef, resourceRef);

            if (existingRoleAssignment.isPresent()) {
                throw new AssignmentAlreadyExistsException(roleRef.toString(), resourceRef.toString());
            }
            Role role = roleRepository.findById(roleRef).get();
            assignment.setRoleName(role.getRoleName());
            assignment.setRoleType(role.getRoleType());
        }
        String assignmentIdSuffix = userRef != null ? userRef + "_user": roleRef + "_role";

        Resource resource = resourceRepository.findById(resourceRef).get();
        assignment.setResourceName(resource.getResourceName());
        assignment.setAssignmentId(resourceRef.toString() + "_" + assignmentIdSuffix);
        assignment.setAzureAdGroupId(resource.getAzureAdGroupId());


        log.info("Trying to save assignment {}", assignment.getAssignmentId());
        Assignment newAssignment = assignmentRepository.save(assignment);
        simpeAssignmentService.process(newAssignment);
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

    private Runnable onSaveNewAssignment(Assignment assignment) {
        return () -> {
            Assignment nesAssignment = assignmentRepository.save(assignment);
            simpeAssignmentService.process(assignment);
        };
    }

    public void deleteAssignment(Long id) {
        Assignment assignment= assignmentRepository.getById(id);
        assignmentRepository.deleteById(id);
    }
}

