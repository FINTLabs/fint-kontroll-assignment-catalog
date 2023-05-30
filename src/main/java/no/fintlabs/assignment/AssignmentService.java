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

    public Mono<Assignment> createNewAssignment(Assignment assignment) {
        String resourceRef = assignment.getResourceRef();
        //TODO: check if the assignment is for user or role
        String userRef = assignment.getUserRef();
        assignment.setAssignmentId(resourceRef + "_" + userRef);
        log.info("Trying to save assignment {}", assignment.getAssignmentId());
        Assignment newsAssignment = assignmentRepository.save(assignment);
        simpeAssignmentService.process(newsAssignment);
        return Mono.just(newsAssignment);
    }
    public Flux<Assignment> getAllAssignments(){
        List<Assignment> allAssignments = assignmentRepository.findAll().stream().collect(Collectors.toList());
        return Flux.fromIterable(allAssignments);
    }
    public Mono<Assignment> findAssignmentById(Long id) {
        Assignment assignment = assignmentRepository.findById(id).orElse(new Assignment());
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
}

