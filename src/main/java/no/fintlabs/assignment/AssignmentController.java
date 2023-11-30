package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.OpaApiClient;
import no.fintlabs.opa.OpaService;
import no.fintlabs.opa.OpaUtils;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;
    private final OpaService opaService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentResponseFactory assignmentResponseFactory;

    public AssignmentController(AssignmentService assignmentService, OpaService opaService, AssignmentRepository assignmentRepository, AssignmentResponseFactory assignmentResponseFactory) {

        this.assignmentService = assignmentService;
        this.opaService = opaService;
        this.assignmentRepository = assignmentRepository;
        this.assignmentResponseFactory = assignmentResponseFactory;
    }

    @GetMapping()
    public ResponseEntity<Map<String,Object>> getSimpleAssignments(@AuthenticationPrincipal Jwt jwt,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                   @RequestParam(defaultValue = "ALLTYPES", required = false) String userType
    ) {

        return assignmentResponseFactory.toResponseEntity(FintJwtEndUserPrincipal.from(jwt),page, size, userType);
    }

    @GetMapping("{id}")
    public DetailedAssignment getAssignmentById(@PathVariable Long id){
        log.info("Fetching assignment info for : "+ id.toString());
        return  assignmentService.findAssignmentById(id);
    }
    @PostMapping()
    public ResponseEntity<Assignment> createAssignment(@Valid @RequestBody NewAssignmentRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(request.resourceRef)
                .organizationUnitId(request.organizationUnitId)
                .build();


        if (request.userRef != null) {
            assignment.setUserRef(request.userRef);
        }
        if (request.roleRef != null) {
            assignment.setRoleRef(request.roleRef);
        }

        log.info("Trying to create new assignment for resource {} and {}"
                , request.getResourceRef()
                , request.userRef != null ? "user " + request.getUserRef() : "role " + request.getRoleRef()
        );

        try {
            Assignment newAssignment = assignmentService.createNewAssignment(assignment);
            return new ResponseEntity<>(newAssignment, HttpStatus.CREATED);
        }
        catch (AssignmentAlreadyExistsException exception)
        {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, exception.getMessage(),exception
            );
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteAssignment(@PathVariable Long id) {
        //log.info("Creating new assignment for resource {} and user {}", assignment.getResourceRef(), assignment.getUserRef());
        assignmentService.deleteAssignment(id);
        return new ResponseEntity<>(HttpStatus.GONE);
    }

}
