package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentResponseFactory assignmentResponseFactory;

    public AssignmentController(AssignmentService assignmentService, AssignmentRepository assignmentRepository, AssignmentResponseFactory assignmentResponseFactory) {

        this.assignmentService = assignmentService;
        this.assignmentRepository = assignmentRepository;
        this.assignmentResponseFactory = assignmentResponseFactory;
    }

    @GetMapping()
    public ResponseEntity<Map<String,Object>> getSimpleAssignments(@AuthenticationPrincipal Jwt jwt,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size ) {

        return assignmentResponseFactory.toResponseEntity(FintJwtEndUserPrincipal.from(jwt),page, size);
    }

    @GetMapping("{id}")
    public Mono<DetailedAssignment> getAssignmentById(@PathVariable Long id){
        log.info("Fetching assignment info for : "+ id.toString());
        return  assignmentService.findAssignmentById(id);
    }
    //TODO Make it return response entity as Mono?
    @PostMapping()
    public ResponseEntity<Assignment> createAssignment(@RequestBody Assignment assignment) {
        log.info("Creating new assignment for resource {} and user {}", assignment.getResourceRef(), assignment.getUserRef());
        Assignment newAssignment = assignmentService.createNewAssignment(assignment);
        return new ResponseEntity<Assignment>(newAssignment, HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteAssignment(@PathVariable Long id) {
        //log.info("Creating new assignment for resource {} and user {}", assignment.getResourceRef(), assignment.getUserRef());
        assignmentService.deleteAssignment(id);
        return new ResponseEntity<>(HttpStatus.GONE);
    }

}
