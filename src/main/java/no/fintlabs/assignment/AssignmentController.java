package no.fintlabs.assignment;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.util.OnlyDevelopers;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final UpdateAssignedResourcesService updateAssignedResourcesService;
    private final ResourceService resourceService;


    public AssignmentController(AssignmentService assignmentService,
                                AssignmentResponseFactory assignmentResponseFactory,
                                FlattenedAssignmentService flattenedAssignmentService,
                                AssigmentEntityProducerService assigmentEntityProducerService,
                                AuthManager authManager,
                                MembershipService membershipService,
                                ResourceRepository resourceRepository,
                                UpdateAssignedResourcesService updateAssignedResourcesService, ResourceService resourceService) {

        this.assignmentService = assignmentService;
        this.assignmentResponseFactory = assignmentResponseFactory;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
        this.authManager = authManager;
        this.membershipService = membershipService;
        this.resourceRepository = resourceRepository;
        this.updateAssignedResourcesService = updateAssignedResourcesService;
        this.resourceService = resourceService;
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
        validateResource(request); //TODO is this correct validation?
        validateOrganizationUnitId(request);

        try {
            Assignment newAssignment =
                    assignmentService.createNewAssignment(request.resourceRef, request.organizationUnitId, request.userRef, request.roleRef);
            return new ResponseEntity<>(newAssignment.toSimpleAssignment(), HttpStatus.CREATED);
        } catch (AssignmentAlreadyExistsException exception) {
            throw new ConflictException("Assignment already exists");
        }
    }

    @OnlyDevelopers
    @PostMapping("/republish")
    public ResponseEntity<HttpStatus> republishAllAssignments() {

        log.info("Republishing all assignments");

        List<FlattenedAssignment> allAssignments = flattenedAssignmentService.getAllFlattenedAssignments();
        allAssignments.forEach(assigmentEntityProducerService::rePublish);

        log.info("Republishing all assignments done");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignments/{sync}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignments(@AuthenticationPrincipal Jwt jwt, @PathVariable("sync") String sync) {
        boolean isSync;
        isSync = sync == null || sync.isEmpty();

        long start = System.currentTimeMillis();
        log.info("Starting to sync all assignments");

        List<Assignment> allAssignments = assignmentService.getAssignments();
        allAssignments.forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, isSync));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync all assignments: " + (end - start) + " ms");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignment/{id}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        long start = System.currentTimeMillis();
        log.info("Starting to sync assignment {}", id);

        assignmentService.getAssignmentById(id)
                .ifPresent(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, false));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync assignment {}: " + (end - start) + " ms", id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignments/resource/{id}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignmentsByResourceId(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long resourceId) {
        log.info("Starting to sync assignments for resource: {}", resourceId);

        assignmentService.getActiveAssignmentsByResource(resourceId)
                .forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, false));

        log.info("Started syncing all flattened assignments for resource: {}", resourceId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/republishuconfirmedflattenedassignments")
    public ResponseEntity<HttpStatus> republishUnconfirmedFlattenedAssignments() {
        log.info("Starting to sync all unconfirmed assignments");

        flattenedAssignmentService.republishUnconfirmedFlattenedAssignments();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/republishselectedflattenedassignments")
    public ResponseEntity<HttpStatus> republishSelectedFlattenedAssignments(@Valid @RequestBody List<Long> ids) {
        log.info("Selected {} flattened assignment to publish", ids.size());

        flattenedAssignmentService.republishSelectedFlattenedAssignments(ids);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @OnlyDevelopers
    @PostMapping("/publishallflattenedassignments/resource/{id}")
    public ResponseEntity<HttpStatus> publishAllFlattenedAssignmentsByResourceId(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long resourceId) {
        log.info("Starting to publish flattened assignments for resource: {}", resourceId);

        assignmentService.getActiveAssignmentsByResource(resourceId)
                .forEach(flattenedAssignmentService::publishAllActive);

        log.info("Finished publishing all flattened assignments for resource: {}", resourceId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncandpublishflattenedassignmentsallresources")
    public ResponseEntity<HttpStatus> syncAndPublishFlattenedassignmentsAllResources(@AuthenticationPrincipal Jwt jwt) {
        log.info("Starting to sync and publish flattenedAssignments all resources");

        resourceService.findAll().forEach(resource -> {
            log.info("Starting to publish flattened assignments for resource: {}", resource);

            assignmentService.getActiveAssignmentsByResource(resource.getId())
                    .forEach(assignment -> {
                        flattenedAssignmentService.syncFlattenedAssignments(assignment, false);
                        flattenedAssignmentService.publishAllActive(assignment);
                    });
            log.info("Finished publishing all flattened assignments for resource: {}", resource);
        });

        log.info("Finished publishing flattened assignments all resources");


        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/syncflattenedassignment/user/{id}")
    public ResponseEntity<HttpStatus> syncFlattenedAssignmentByUserId(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        long start = System.currentTimeMillis();
        log.info("Starting to sync assignment by userid {}", id);

        List<Assignment> allAssignments = assignmentService.getAssignmentsByUser(id);
        allAssignments.forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, false));

        long end = System.currentTimeMillis();
        log.info("Time taken to sync assignments by userid {}: " + (end - start) + " ms", id);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<HttpStatus> deleteAssignment(@PathVariable("id") Long id) {
        log.info("Deleting assignment with id {}", id);
        try {
            assignmentService.deleteAssignment(id);
        } catch (UserNotFoundException userNotFoundException) {
            log.error("Logged in user not found in the users table", userNotFoundException);

            throw new ResourceNotFoundException("Logged in user not found in the users table");
        }
        return new ResponseEntity<>(HttpStatus.GONE);
    }

    @OnlyDevelopers
    @PostMapping("/syncunconfirmedflattenedassignment/{assignmentId}")
    public ResponseEntity<HttpStatus> syncUnconfirmedFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable("assignmentId") Long assignmentId) {

        log.info("Syncing unconfirmed flattened assignments for assignmentId {}", assignmentId);

        flattenedAssignmentService.getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmedByAssignmentId(assignmentId)
                .forEach(assigmentEntityProducerService::publish);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncdeletedflattenedassignment/{assignmentId}")
    public ResponseEntity<HttpStatus> syncDeletedFlattenedAssignmentById(@AuthenticationPrincipal Jwt jwt, @PathVariable("assignmentId") Long assignmentId) {

        log.info("Publishing deleted flattened assignments");

        flattenedAssignmentService.getFlattenedAssignmentsDeletedNotConfirmedByAssignmentId(assignmentId)
                .forEach(assigmentEntityProducerService::publishDeletion);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncassignmentsformemberships")
    public ResponseEntity<HttpStatus> syncAssignmentsForMemberships(@AuthenticationPrincipal Jwt jwt, @RequestBody List<String> membershipIds) {

        log.info("Syncing assignments for memberships");

        membershipService.syncAssignmentsForMemberships(membershipIds);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/syncassignmentsmissingidentityprovideruserobjectid")
    public ResponseEntity<HttpStatus> syncAssignmentsMissingIdentityProviderUserObjectId(@AuthenticationPrincipal Jwt jwt) {

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

    @OnlyDevelopers
    @PostMapping("/update-assignments-applicationresourcelocationorgunit")
    public ResponseEntity<HttpStatus> updateApplicationResourceLocationOrgUnitOnAssignments(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateAllResourceLocationOrgUnits updateAll) {


        boolean updateAllResourceLocationOrgUnits = updateAll.updateAllResourceLocationOrgUnits != null && updateAll.updateAllResourceLocationOrgUnits;
        long start = System.currentTimeMillis();
        log.info("Start updating application resource location org unit for all assignments {}", updateAllResourceLocationOrgUnits ? "" : "where location org unit is missing");

        Set<Long> ids = assignmentService.getAssignments()
                .stream()
                .filter(assignment -> {
                    if (updateAllResourceLocationOrgUnits) {
                        return true;
                    }
                    return assignment.getApplicationResourceLocationOrgUnitId() == null;
                })
                .map(Assignment::getId)
                .collect(Collectors.toSet());

        log.info("Found {} assignments where application resource location org unit wil be updated", ids.size());

        if (!ids.isEmpty()) {
            assignmentService.updateAssignmentsWithApplicationResourceLocationOrgUnitAsync(ids);
        }

        long end = System.currentTimeMillis();
        log.info("Time taken to update {} assignments missing application resource location org unit: {} ms", ids.size(), end - start);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
    @PostMapping("/update-assigned-resources-usage")
    public ResponseEntity<HttpStatus> updateAssignedResoursesUsage(@AuthenticationPrincipal Jwt jwt) {

        long start = System.currentTimeMillis();
        log.info("Start updating assignedResoursesUsage of assignment");

        updateAssignedResourcesService.updateAssignedResources();

        long end = System.currentTimeMillis();
        log.info("Time taken to update all resourceAvailability entities for alle resources : {} ms", end - start);


        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void validateResource(NewAssignmentRequest request) {
        if (request.resourceRef == null) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "ResourceRef must be set");
        }

        resourceRepository.findById(request.resourceRef)
                .ifPresentOrElse(
                        resource -> {
                            if (resource.getIdentityProviderGroupObjectId() == null) {
                                throw new AssignmentException(HttpStatus.UNPROCESSABLE_ENTITY, "Resource " + request.resourceRef + " does not have azure group id set");
                            }
                        },
                        () -> {
                            log.error("Resource: {} not found", request.resourceRef);
                            throw new AssignmentException(HttpStatus.NOT_FOUND, "Resource " + request.resourceRef + " not found");
                        }
                );
    }

    private void validateOrganizationUnitId(NewAssignmentRequest request) {
        if (request.organizationUnitId == null || request.organizationUnitId.isEmpty()) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "OrganizationUnitId must be set and not empty");
        }
    }

    private void validateUserRoleRefs(NewAssignmentRequest request) {
        if (request.userRef != null && request.roleRef != null) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "Either userRef or roleRef must be set, not both");
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class UpdateAllResourceLocationOrgUnits {
        private Boolean updateAllResourceLocationOrgUnits;
    }

}

