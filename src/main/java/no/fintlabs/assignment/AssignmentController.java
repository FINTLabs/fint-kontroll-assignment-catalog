package no.fintlabs.assignment;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final OpaService opaService;
    private final AssignmentResponseFactory assignmentResponseFactory;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public AssignmentController(AssignmentService assignmentService, OpaService opaService,
                                AssignmentResponseFactory assignmentResponseFactory,
                                FlattenedAssignmentService flattenedAssignmentService,
                                AssigmentEntityProducerService assigmentEntityProducerService) {

        this.assignmentService = assignmentService;
        this.opaService = opaService;
        this.assignmentResponseFactory = assignmentResponseFactory;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    @GetMapping()
    public ResponseEntity<Map<String, Object>> getSimpleAssignments(@AuthenticationPrincipal Jwt jwt,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog" +
                                                                                                 ".pagesize:20}")
                                                                    int size,
                                                                    @RequestParam(defaultValue = "ALLTYPES", required = false)
                                                                    String userType
    ) {

        return assignmentResponseFactory.toResponseEntity(FintJwtEndUserPrincipal.from(jwt), page, size, userType);
    }

    @GetMapping("{id}")
    public DetailedAssignment getAssignmentById(@PathVariable Long id) {
        log.info("Fetching assignment info for : " + id.toString());
        return assignmentService.findAssignmentById(id);
    }

    @PostMapping()
    public ResponseEntity<Assignment> createAssignment(@Valid @RequestBody NewAssignmentRequest request) {
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
        } catch (AssignmentAlreadyExistsException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, exception.getMessage(), exception
            );
        }
    }

    @PostMapping("/republish")
    public ResponseEntity<HttpStatus> republishAllAssignments() {
        log.info("Republishing all assignments");

        List<FlattenedAssignment> allAssignments = flattenedAssignmentService.getAllFlattenedAssignments();
        allAssignments.forEach(assigmentEntityProducerService::publish);

        log.info("Republishing all assignments done");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteAssignment(@PathVariable Long id) {
        //log.info("Creating new assignment for resource {} and user {}", assignment.getResourceRef(), assignment.getUserRef());
        assignmentService.deleteAssignment(id);
        return new ResponseEntity<>(HttpStatus.GONE);
    }

}
