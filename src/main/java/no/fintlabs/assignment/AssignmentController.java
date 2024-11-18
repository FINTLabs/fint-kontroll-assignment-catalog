package no.fintlabs.assignment;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.resource.ResourceRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AuthManager authManager;
    private final AssignmentResponseFactory assignmentResponseFactory;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;
    private final MembershipService membershipService;
    private final ResourceRepository resourceRepository;


    public AssignmentController(AssignmentService assignmentService,
                                AssignmentResponseFactory assignmentResponseFactory,
                                FlattenedAssignmentService flattenedAssignmentService,
                                AssigmentEntityProducerService assigmentEntityProducerService,
                                AuthManager authManager,
                                MembershipService membershipService,
                                ResourceRepository resourceRepository) {

        this.assignmentService = assignmentService;
        this.assignmentResponseFactory = assignmentResponseFactory;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.authManager = authManager;
        this.membershipService = membershipService;
        this.resourceRepository = resourceRepository;
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
    public ResponseEntity<SimpleAssignment> createAssignment(@Valid @RequestBody NewAssignmentRequest request) {
        log.info("Creating assignment. Request - userRef: {}, roleRef: {}, resourceRef: {}, organizationUnitId: {}", request.userRef, request.roleRef, request.resourceRef, request.organizationUnitId);
        validateUserRoleRefs(request);
        validateResource(request);
        validateOrganizationUnitId(request);

        try {
            Assignment newAssignment =
                    assignmentService.createNewAssignment(request.resourceRef, request.organizationUnitId, request.userRef, request.roleRef);
            return new ResponseEntity<>(newAssignment.toSimpleAssignment(), HttpStatus.CREATED);
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

        assignmentService.getAssignmentById(id)
                .ifPresent(assignment -> flattenedAssignmentService.createFlattenedAssignments(assignment, false));

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
        allAssignments.forEach(assignment -> flattenedAssignmentService.createFlattenedAssignments(assignment, false));

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
    public ResponseEntity<HttpStatus> syncDeletedFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long assignmentId) {
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

    @PostMapping("/syncassignmentsmissingidentityprovideruserobjectid")
    public ResponseEntity<HttpStatus> syncAssignmentsMissingIdentityProviderUserObjectId(@AuthenticationPrincipal Jwt jwt) {
        if (!validateIsAdmin(jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        long start = System.currentTimeMillis();
        log.info("Syncing assignments missing identityProviderUserObjectId");

        Set<Long> ids = flattenedAssignmentService.getIdsMissingIdentityProviderUserObjectId();

        log.info("Found {} flattened assignments missing identityProviderUserObjectId", ids.size());

        if (!ids.isEmpty()) {
            log.info("Deleting {} flattened assignments missing identityProviderUserObjectId", ids.size());

            flattenedAssignmentService.deactivateFlattenedAssignments(ids);

            log.info("Done deleting {} flattened assignments missing identityProviderUserObjectId", ids.size());
        }

        long end = System.currentTimeMillis();
        log.info("Time taken to sync assignments missing identity provider user object id: " + (end - start) + " ms");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void validateResource(NewAssignmentRequest request) {
        if (request.resourceRef == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ResourceRef must be set");
        }

        resourceRepository.findById(request.resourceRef)
                .ifPresentOrElse(
                        resource -> {
                            if (resource.getIdentityProviderGroupObjectId() == null) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource " + request.resourceRef + " does not have azure group id set");
                            }
                        },
                        () -> {
                            log.error("Resource: {} not found", request.resourceRef);
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource " + request.resourceRef + " not found");
                        }
                );
    }

    private void validateOrganizationUnitId(NewAssignmentRequest request) {
        if (request.organizationUnitId == null || request.organizationUnitId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OrganizationUnitId must be set and not empty");
        }
    }

    private void validateUserRoleRefs(NewAssignmentRequest request) {
        if (request.userRef != null && request.roleRef != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either userRef or roleRef must be set, not both");
        }
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
