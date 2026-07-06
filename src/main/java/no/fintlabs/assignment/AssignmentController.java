package no.fintlabs.assignment;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.device.assignment.DeviceAssignmentService;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.util.OnlyDevelopers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;
    private final MembershipService membershipService;
    private final ResourceRepository resourceRepository;
    private final UpdateAssignedResourcesService updateAssignedResourcesService;
    private final ResourceService resourceService;
    private final DeviceAssignmentService deviceAssignmentService;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final DeviceGroupRepository deviceGroupRepository;

    @PostMapping()
    public ResponseEntity<SimpleAssignment> createAssignment(@Valid @RequestBody NewAssignmentRequest request) {
        log.info("Creating assignment. Request - userRef: {}, roleRef: {}, deviceGroupRef: {}, resourceRef: {}, organizationUnitId: {}",
                request.userRef, request.roleRef, request.deviceGroupRef, request.resourceRef, request.organizationUnitId);
        validateAssignmentTarget(request);
        validateResource(request);
        validateOrganizationUnitId(request);
        try {
            Assignment newAssignment = createNewAssignment(request);
            return new ResponseEntity<>(newAssignment.toSimpleAssignment(), HttpStatus.CREATED);
        } catch (AssignmentAlreadyExistsException exception) {
            throw new ConflictException("Assignment already exists");
        }
    }

    private Assignment createNewAssignment(NewAssignmentRequest request) {
        if (request.deviceGroupRef != null) {
            Assignment newAssignment = deviceAssignmentService.createNewAssignment(
                    request.resourceRef,
                    request.organizationUnitId,
                    request.deviceGroupRef
            );
            flattenedDeviceAssignmentService.createAndPublishFlattenedAssignments(newAssignment);
            return newAssignment;
        }

        return assignmentService.createNewAssignment(request.resourceRef, request.organizationUnitId, request.userRef, request.roleRef);
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

        List<Assignment> allAssignments = assignmentService.getAllUserAssignments();
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

        assignmentService.getActiveUserAssignmentsByResource(resourceId)
                .forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, false));

        log.info("Started syncing all flattened assignments for resource: {}", resourceId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping({"/resource/{id}/devicegroups", "/resource/{id}/deviceGroups"})
    public ResponseEntity<Page<DeviceGroup>> getDeviceGroupsByResourceId(
            @PathVariable("id") Long resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size
    ) {
        log.info("Fetching device groups for resource {} with page {} and size {}", resourceId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Assignment> activeAssignments = deviceAssignmentService.getActiveAssignmentsByResource(resourceId);
        log.info("Found {} active device assignments for resource {}", activeAssignments.size(), resourceId);

        List<Long> deviceGroupIds = activeAssignments
                .stream()
                .map(Assignment::getDeviceGroupRef)
                .distinct()
                .toList();

        Page<Long> deviceGroupIdPage = toPage(deviceGroupIds, pageable);
        List<DeviceGroup> deviceGroups = deviceGroupRepository.findAllById(deviceGroupIdPage.getContent());

        log.info(
                "Returning {} device groups for resource {}. Distinct device groups: {}, page: {}, size: {}, total pages: {}",
                deviceGroups.size(),
                resourceId,
                deviceGroupIds.size(),
                page,
                size,
                deviceGroupIdPage.getTotalPages()
        );

        return new ResponseEntity<>(new PageImpl<>(deviceGroups, pageable, deviceGroupIds.size()), HttpStatus.OK);
    }

    @GetMapping("/devicegroup/{id}/resources")
    public ResponseEntity<Page<Resource>> getResourcesByDeviceGroupId(
            @PathVariable("id") Long deviceGroupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size
    ) {
        log.info("Fetching resources for device group {} with page {} and size {}", deviceGroupId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Assignment> activeAssignments = deviceAssignmentService.getActiveAssignmentsByDeviceGroup(deviceGroupId);
        log.info("Found {} active device assignments for device group {}", activeAssignments.size(), deviceGroupId);

        List<Long> resourceIds = activeAssignments
                .stream()
                .map(Assignment::getResourceRef)
                .distinct()
                .toList();

        Page<Long> resourceIdPage = toPage(resourceIds, pageable);
        List<Resource> resources = resourceRepository.findAllById(resourceIdPage.getContent());

        log.info(
                "Returning {} resources for device group {}. Distinct resources: {}, page: {}, size: {}, total pages: {}",
                resources.size(),
                deviceGroupId,
                resourceIds.size(),
                page,
                size,
                resourceIdPage.getTotalPages()
        );

        return new ResponseEntity<>(new PageImpl<>(resources, pageable, resourceIds.size()), HttpStatus.OK);
    }

    private Page<Long> toPage(List<Long> ids, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), ids.size());

        if (start >= ids.size()) {
            return new PageImpl<>(List.of(), pageable, ids.size());
        }

        return new PageImpl<>(ids.subList(start, end), pageable, ids.size());
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

        assignmentService.getActiveUserAssignmentsByResource(resourceId)
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

            assignmentService.getActiveUserAssignmentsByResource(resource.getId())
                    .forEach(assignment -> {
                        flattenedAssignmentService.syncFlattenedAssignments(assignment, false);
                        flattenedAssignmentService.publishAllActive(assignment);
                    });
            log.info("Finished publishing all flattened assignments for resource: {}", resource);
        });

        log.info("Finished publishing flattened assignments all resources");


        return new ResponseEntity<>(HttpStatus.OK);
    }

    @OnlyDevelopers
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

        Set<Long> ids = assignmentService.getAllUserAssignments()
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
    // runs for both user and device assignments
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

    private void validateAssignmentTarget(NewAssignmentRequest request) {
        int assignmentTargetCount = 0;
        assignmentTargetCount += request.userRef == null ? 0 : 1;
        assignmentTargetCount += request.roleRef == null ? 0 : 1;
        assignmentTargetCount += request.deviceGroupRef == null ? 0 : 1;

        if (assignmentTargetCount == 0) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "Either userRef, roleRef or deviceGroupRef must be set");
        }

        if (request.userRef != null && request.roleRef != null && request.deviceGroupRef == null) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "Cannot assign both role and user");
        }

        if (assignmentTargetCount > 1) {
            throw new AssignmentException(HttpStatus.BAD_REQUEST, "Only one of userRef, roleRef or deviceGroupRef can be set");
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class UpdateAllResourceLocationOrgUnits {
        private Boolean updateAllResourceLocationOrgUnits;
    }

}
