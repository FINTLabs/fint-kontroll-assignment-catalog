package no.fintlabs.assignment;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.assignment.device.DeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroupAssignment;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.util.OnlyDevelopers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {
    private static final String DEVELOPER_ENDPOINTS_TAG = "Developer endpoints";

    private final AssignmentService assignmentService;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final MembershipService membershipService;
    private final UpdateAssignedResourcesService updateAssignedResourcesService;
    private final DeviceAssignmentService deviceAssignmentService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public SimpleAssignment createAssignment(@Valid @RequestBody NewAssignmentRequest request) {
        log.info("Creating assignment. Request - userRef: {}, roleRef: {}, deviceGroupRef: {}, resourceRef: {}, organizationUnitId: {}",
                request.userRef, request.roleRef, request.deviceGroupRef, request.resourceRef, request.organizationUnitId);
        return createNewAssignment(request).toSimpleAssignment();
    }

    @GetMapping({"/resource/{id}/devicegroups", "/resource/{id}/deviceGroups"})
    @ResponseStatus(HttpStatus.OK)
    public Page<DeviceGroupAssignment> getDeviceGroupsByResourceId(
            @PathVariable("id") Long resourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
            @RequestParam(value = "search", required = false) String search
    ) {
        log.info("Fetching device groups for resource {} with page {}, size {} and search {}", resourceId, page, size, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
        Page<DeviceGroupAssignment> deviceGroupAssignments = deviceAssignmentService.findDeviceGroupAssignmentsForResource(
                resourceId,
                search,
                pageable
        );

        log.info(
                "Returning {} device groups for resource {}. Distinct device groups: {}, page: {}, size: {}, total pages: {}",
                deviceGroupAssignments.getNumberOfElements(),
                resourceId,
                deviceGroupAssignments.getTotalElements(),
                page,
                size,
                deviceGroupAssignments.getTotalPages()
        );

        return deviceGroupAssignments;
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.GONE)
    public void deleteAssignment(@PathVariable("id") Long id) {
        log.info("Deleting assignment with id {}", id);
        deleteAssignmentByType(id);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Republish all active user memberships",
            description = "Finds active flattened user assignments and republishes each associated user resource-group membership exactly once, regardless of its previous Kafka or Entra send status. Device assignments are not included."
    )
    @PostMapping("/republish")
    @ResponseStatus(HttpStatus.OK)
    public void republishAllActiveUserMemberships() {

        log.info("Republishing all active user memberships");

        flattenedAssignmentService.republishAllActiveUserMemberships();

        log.info("Republishing all active user memberships done");

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync all flattened assignments",
            description = "Rebuilds flattened data for all user assignments without publishing pending user memberships."
    )
    @PostMapping("/syncflattenedassignments")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignments() {
        syncAllFlattenedAssignments(false);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync all flattened assignments and publish pending memberships",
            description = "Rebuilds flattened data for all user assignments and publishes each resulting user membership that has not previously been sent."
    )
    @PostMapping("/syncflattenedassignments/publish-pending-memberships")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentsAndPublishPendingMemberships() {
        syncAllFlattenedAssignments(true);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync flattened assignment and publish pending membership by assignment id",
            description = "Rebuilds flattened data for one user assignment and publishes its resulting user membership if it has not previously been sent."
    )
    @PostMapping("/syncflattenedassignment/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentAndPublishPendingMembershipById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
        log.info("Starting to sync assignment {}", id);

        assignmentService.getAssignmentById(id)
                .ifPresent(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, true));

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync flattened assignments and publish pending memberships by resource id",
            description = "Rebuilds flattened data for active user assignments connected to one resource and publishes resulting user memberships that have not previously been sent."
    )
    @PostMapping("/syncflattenedassignments/resource/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentsAndPublishPendingMembershipsByResourceId(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long resourceId) {
        log.info("Starting to sync assignments for resource: {}", resourceId);

        assignmentService.getActiveUserAssignmentsByResource(resourceId)
                .forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, true));

        log.info("Started syncing all flattened assignments for resource: {}", resourceId);

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Republish unconfirmed user memberships",
            description = "Republishes each user resource-group membership that is awaiting Entra confirmation exactly once. Active memberships are sent as ADD commands and inactive memberships as REMOVE commands. Memberships in ERROR are excluded. Device assignments are not included."
    )
    @PostMapping("/republishunconfirmedflattenedassignments")
    @ResponseStatus(HttpStatus.OK)
    public void republishUnconfirmedUserEntraMemberships() {
        log.info("Republishing all user memberships awaiting Entra confirmation");

        flattenedAssignmentService.republishUnconfirmedUserEntraMemberships();

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Republish selected user memberships",
            description = "Finds the user memberships associated with the provided flattened assignment ids and publishes each membership once, regardless of its Entra status. Active memberships are sent as ADD commands and inactive memberships as REMOVE commands."
    )
    @PostMapping("/republishselectedflattenedassignments")
    @ResponseStatus(HttpStatus.OK)
    public void republishSelectedFlattenedAssignments(@Valid @RequestBody List<Long> ids) {
        log.info("Republishing user memberships for {} selected flattened assignments", ids.size());

        flattenedAssignmentService.republishSelectedFlattenedAssignments(ids);

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync direct user assignments and publish pending memberships by user id",
            description = "Rebuilds flattened data for all non-removed direct assignments of one user and publishes resulting user memberships that have not previously been sent. Role-based assignments are not included."
    )
    @PostMapping("/syncflattenedassignment/user/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void syncDirectUserAssignmentsAndPublishPendingMembershipsByUserId(@PathVariable("id") Long userId) {
        log.info("Starting to sync direct assignments for user {}", userId);

        assignmentService.getAssignmentsByUser(userId)
                .forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, true));

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Republish unconfirmed user memberships by assignment id",
            description = "Republishes each user membership associated with one Assignment.id that is awaiting Entra confirmation exactly once. Active memberships are sent as ADD commands and inactive memberships as REMOVE commands. ERROR memberships and device assignments are excluded."
    )
    @PostMapping("/syncunconfirmedflattenedassignment/{assignmentId}")
    @ResponseStatus(HttpStatus.OK)
    public void republishUnconfirmedUserMembershipsByAssignmentId(@PathVariable("assignmentId") Long assignmentId) {

        log.info("Republishing unconfirmed user memberships for assignment {}", assignmentId);

        flattenedAssignmentService.republishUnconfirmedUserEntraMembershipsByAssignmentId(assignmentId);

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Sync role-membership assignments",
            description = "Rebuilds flattened user assignments for the supplied Membership.id values and publishes resulting memberships that have not previously been sent. Each membership id is processed once; unknown ids fail the request. Role memberships without an Entra user id and device assignments are not included."
    )
    @PostMapping("/syncassignmentsformemberships")
    @ResponseStatus(HttpStatus.OK)
    public void syncAssignmentsForMemberships(@Valid @RequestBody @NotEmpty List<@NotBlank String> membershipIds) {

        log.info("Syncing assignments for {} requested memberships", membershipIds.size());

        membershipService.syncAssignmentsForMemberships(membershipIds);

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Backfill application resource location org units for active user assignments",
            description = "Updates all active user and role assignments when updateAllResourceLocationOrgUnits is true; otherwise updates only those without a location org unit. It synchronizes affected flattened user assignments and recalculates their usage. No Entra membership commands are published. Device and removed assignments are excluded."
    )
    @PostMapping("/update-assignments-applicationresourcelocationorgunit")
    @ResponseStatus(HttpStatus.OK)
    public void updateApplicationResourceLocationOrgUnitOnAssignments(@RequestBody UpdateAllResourceLocationOrgUnits updateAll) {


        boolean updateAllResourceLocationOrgUnits = updateAll.updateAllResourceLocationOrgUnits;
        log.info("Start updating application resource location org unit for all assignments {}", updateAllResourceLocationOrgUnits ? "" : "where location org unit is missing");

        Set<Long> ids = assignmentService.getAllActiveUserAssignments()
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
            assignmentService.updateAssignmentsWithApplicationResourceLocationOrgUnit(ids);
        }

    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Reconcile assigned resource usage",
            description = "Recalculates user and device membership usage for every resource and resource location from active flattened assignments. It does not synchronize assignments or publish Entra membership commands; availability updates are published after each resource reconciliation commits."
    )
    @PostMapping("/update-assigned-resources-usage")
    @ResponseStatus(HttpStatus.OK)
    public void reconcileAssignedResourcesUsage() {

        log.info("Starting assigned resource usage reconciliation");

        updateAssignedResourcesService.updateAssignedResources();

    }

    private Assignment createNewAssignment(NewAssignmentRequest request) {
        if (request.deviceGroupRef != null) {
            return deviceAssignmentService.createNewAssignment(
                    request.resourceRef,
                    request.organizationUnitId,
                    request.deviceGroupRef
            );
        }

        return assignmentService.createNewAssignment(request.resourceRef, request.organizationUnitId, request.userRef, request.roleRef);
    }

    private void syncAllFlattenedAssignments(boolean publishPendingMemberships) {
        log.info("Starting to sync all assignments. publishPendingMemberships={}", publishPendingMemberships);

        List<Assignment> allAssignments = assignmentService.getAllUserAssignments();
        allAssignments.forEach(assignment -> flattenedAssignmentService.syncFlattenedAssignments(assignment, publishPendingMemberships));
    }

    private void deleteAssignmentByType(Long id) {
        Assignment assignment = assignmentService.getAssignmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));

        if (assignment.getDeviceGroupRef() != null) {
            deviceAssignmentService.deleteAssignment(id);
            return;
        }

        assignmentService.deleteAssignment(id);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateAllResourceLocationOrgUnits {
        private boolean updateAllResourceLocationOrgUnits;
    }

}
