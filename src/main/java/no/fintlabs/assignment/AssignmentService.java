package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssignmentService {
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SimpeAssignmentService simpeAssignmentService;
    private final ResourceRepository resourceRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, SimpeAssignmentService simpeAssignmentService, ResourceRepository resourceRepository,
                             UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.simpeAssignmentService = simpeAssignmentService;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public Assignment createNewAssignment(Assignment assignment) {
        //TODO: handle Optional.Empty return
        //TODO: Handle both roleRef and userRef null
        Long userRef = assignment.getUserRef();
        User user = userRepository.findById(userRef).get();
        assignment.setUserFirstName(user.getFirstName());
        assignment.setUserLastName(user.getLastName());
        assignment.setUserUserType(user.getUserType());

        String assignmentIdSuffix = userRef + "_user";
//TODO: implement assignment for roles(groups)
//        Long roleRef = assignment.getRoleRef();

//        String assignmentIdSuffix = (userRef != null) ?
//                (userRef + "_user") :
//                (roleRef + "_role");

        Long resourceRef = assignment.getResourceRef();
        Resource resource = resourceRepository.findById(resourceRef).get();
        assignment.setResourceName(resource.getResourceName());

        assignment.setAssignmentId(resourceRef.toString() + "_" + assignmentIdSuffix);


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

