package no.fintlabs.assignment;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.opa.OpaService;
import no.fintlabs.user.UserNotFoundException;
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

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final OpaService opaService;
    private final AuthManager authManager;
    private final AssignmentResponseFactory assignmentResponseFactory;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    private final MembershipService membershipService;


    public AssignmentController(AssignmentService assignmentService, OpaService opaService,
                                AssignmentResponseFactory assignmentResponseFactory,
                                FlattenedAssignmentService flattenedAssignmentService,
                                AssigmentEntityProducerService assigmentEntityProducerService,
                                AuthManager authManager,
                                MembershipService membershipService) {

        this.assignmentService = assignmentService;
        this.opaService = opaService;
        this.assignmentResponseFactory = assignmentResponseFactory;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.authManager = authManager;
        this.membershipService = membershipService;
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

    @PostMapping()
    public ResponseEntity<?> createAssignment(@Valid @RequestBody NewAssignmentRequest request) {
        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(request.resourceRef)
                .organizationUnitId(request.organizationUnitId)
                .build();

        log.info("Request returned - userRef: {}, roleRef: {}, resourceRef: {}, organizationUnitId: {}", request.userRef, request.roleRef, request.resourceRef, request.organizationUnitId);

        if (request.userRef != null && request.roleRef != null) {
            return ResponseEntity.badRequest().body("Either userRef or roleRef must be set, not both");
        }

        if (request.organizationUnitId == null || request.organizationUnitId.isEmpty()) {
            return ResponseEntity.badRequest().body("OrganizationUnitId must be set and not empty");
        }

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
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error creating assignment", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/republish")
    public ResponseEntity<HttpStatus> republishAllAssignments() { //@AuthenticationPrincipal Jwt jwt) {

        //TODO: implement when agreed on security solution
        /*if (!validateIsAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to republish all assignments");
        }*/

        log.info("Republishing all assignments");

        List<FlattenedAssignment> allAssignments = flattenedAssignmentService.getAllFlattenedAssignments();
        allAssignments.forEach(assigmentEntityProducerService::rePublish);

        log.info("Republishing all assignments done");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/syncflattenedassignments")
    public ResponseEntity<HttpStatus> syncFlattenedAssignments(@AuthenticationPrincipal Jwt jwt) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        long start = System.currentTimeMillis();
        log.info("Starting to sync all assignments");

        List<Assignment> allAssignments = assignmentService.getAssignments();
        allAssignments.forEach(assignment -> flattenedAssignmentService.createFlattenedAssignments(assignment, true));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync all assignments: " + (end - start) + " ms");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/syncflattenedassignment/{id}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        long start = System.currentTimeMillis();
        log.info("Starting to sync assignment {}", id);

        assignmentService.getAssignmentsById(id)
                .ifPresent(assignment -> flattenedAssignmentService.createFlattenedAssignments(assignment, true));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync assignment {}: " + (end - start) + " ms", id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/syncflattenedassignment/user/{id}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignmentByUserId(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        long start = System.currentTimeMillis();
        log.info("Starting to sync assignment by userid {}", id);

        List<Assignment> allAssignments = assignmentService.getAssignmentsByUser(id);
        allAssignments.forEach(assignment -> flattenedAssignmentService.createFlattenedAssignments(assignment, true));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync assignments by userid {}: " + (end - start) + " ms", id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteAssignment(@PathVariable Long id) {
        log.info("Deleting assignment with id {}", id);
        try {
            assignmentService.deleteAssignment(id);
        } catch (UserNotFoundException userNotFoundException) {
            log.error("Logged in user not found in the users table", userNotFoundException);

            return ResponseEntity.notFound().build();
        }
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @PostMapping("/syncunconfirmedflattenedassignment/{assignmentId}")
    public ResponseEntity<HttpStatus> syncUnconfirmedFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long assignmentId) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        log.info("Syncing unconfirmed flattened assignments for assignmentId {}", assignmentId);

        flattenedAssignmentService.getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmedByAssignmentId(assignmentId)
                .forEach(assigmentEntityProducerService::publish);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/syncdeletedflattenedassignment/{assignmentId}")
    public ResponseEntity<HttpStatus>  syncDeletedFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long assignmentId) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        log.info("Publishing deleted flattened assignments");

        flattenedAssignmentService.getFlattenedAssignmentsDeletedNotConfirmedByAssignmentId(assignmentId)
                .forEach(assigmentEntityProducerService::publishDeletion);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/syncassignmentsformemberships")
    public ResponseEntity<HttpStatus> syncAssignmentsForMemberships(@AuthenticationPrincipal Jwt jwt, @RequestBody List<String> membershipIds) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        log.info("Syncing assignments for memberships");

        membershipService.syncAssignmentsForMemberships(membershipIds);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private boolean validateIsAdmin(Jwt jwt) {
        boolean hasAdminAdminAccess = authManager.hasAdminAdminAccess(jwt);

        if (!hasAdminAdminAccess) {
            log.error("User does not have admin acccess");
            return false;
        }

        return true;
    }

}
