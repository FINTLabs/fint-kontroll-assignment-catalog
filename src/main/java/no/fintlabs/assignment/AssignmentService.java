package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssignmentService {
    @Autowired
    private AssignmentRepository assignmentRepository;
    private SimpeAssignmentService simpeAssignmentService;

    public AssignmentService(SimpeAssignmentService simpeAssignmentService) {
        this.simpeAssignmentService = simpeAssignmentService;
    }

    public Assignment createNewAssignment(Assignment assignment) {
        Long userRef = assignment.getUserRef();
        Long roleRef = assignment.getRoleRef();
        //TODO: Handle both roleRef and userRef null
        String assignmentIdSuffix = (userRef != null) ?
                (userRef + "_user") :
                (roleRef + "_role");

        Long resourceRef = assignment.getResourceRef();
        assignment.setAssignmentId(resourceRef.toString() + "_" + assignmentIdSuffix);
        log.info("Trying to save assignment {}", assignment.getAssignmentId());
        Assignment newAssignment = assignmentRepository.save(assignment);
        simpeAssignmentService.process(newAssignment);
        return newAssignment;
    }
    public Flux<Assignment> getAllAssignments(){
        List<Assignment> allAssignments = assignmentRepository.findAll().stream().collect(Collectors.toList());
        return Flux.fromIterable(allAssignments);
    }
    public Mono<DetailedAssignment> findAssignmentById(Long id) {
        DetailedAssignment assignment = assignmentRepository
                .findById(id)
                .map(Assignment::toDetailedAssignment)
                .orElse(new DetailedAssignment());
        return Mono.just(assignment);
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

