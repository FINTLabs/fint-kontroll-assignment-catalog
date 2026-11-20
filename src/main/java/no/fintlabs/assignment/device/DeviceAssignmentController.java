package no.fintlabs.assignment.device;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.util.OnlyDevelopers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Maintenance/admin endpoints for device assignments.
 *
 * Normal assignment writes, including device-group assignment create/delete, are handled by
 * {@code AssignmentController} under {@code /api/assignments}.
 */
@Slf4j
@RestController
@RequestMapping("/api/device-assignments")
@RequiredArgsConstructor
@Tag(
        name = "Device assignment maintenance",
        description = "Maintenance/admin endpoints for device assignments. Normal create and delete operations are handled by the generic /api/assignments controller."
)
public class DeviceAssignmentController {
    private static final String DEVICE_ASSIGNMENT_MAINTENANCE_TAG = "Device assignment maintenance";

    private final DeviceAssignmentService deviceAssignmentService;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final UpdateAssignedResourcesService updateAssignedResourcesService;

    @OnlyDevelopers
    @Operation(
            tags = DEVICE_ASSIGNMENT_MAINTENANCE_TAG,
            summary = "Sync all flattened device assignments",
            description = "Rebuilds flattened data for all active device assignments without publishing pending device memberships."
    )
    @PostMapping("/syncflattenedassignments")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignments() {
        syncAllFlattenedAssignments(false);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVICE_ASSIGNMENT_MAINTENANCE_TAG,
            summary = "Sync all flattened device assignments and publish pending memberships",
            description = "Rebuilds flattened data for all active device assignments and publishes each resulting device membership that has not previously been sent."
    )
    @PostMapping("/syncflattenedassignments/publish-pending-memberships")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentsAndPublishPendingMemberships() {
        syncAllFlattenedAssignments(true);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVICE_ASSIGNMENT_MAINTENANCE_TAG,
            summary = "Sync flattened device assignment by assignment id",
            description = "Rebuilds flattened data for one active device assignment and publishes resulting device memberships that have not previously been sent."
    )
    @PostMapping("/syncflattenedassignment/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentById(@PathVariable("id") Long assignmentId) {
        log.info("Starting to sync flattened device assignment {}", assignmentId);

        deviceAssignmentService.getAssignmentById(assignmentId)
                .ifPresent(assignment -> flattenedDeviceAssignmentService.syncFlattenedAssignments(assignment, true));
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVICE_ASSIGNMENT_MAINTENANCE_TAG,
            summary = "Sync flattened device assignments by resource id",
            description = "Rebuilds flattened data for active device assignments connected to one resource and publishes resulting device memberships that have not previously been sent."
    )
    @PostMapping("/syncflattenedassignments/resource/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void syncFlattenedAssignmentsByResourceId(@PathVariable("id") Long resourceId) {
        log.info("Starting to sync device assignments for resource: {}", resourceId);

        deviceAssignmentService.getActiveAssignmentsByResource(resourceId)
                .forEach(assignment -> flattenedDeviceAssignmentService.syncFlattenedAssignments(assignment, true));

        log.info("Started syncing all flattened device assignments for resource: {}", resourceId);
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVICE_ASSIGNMENT_MAINTENANCE_TAG,
            summary = "Reconcile assigned resource usage",
            description = "Recalculates user and device membership usage for every resource and resource location from active flattened assignments. It does not synchronize assignments or publish Entra membership commands; availability updates are published after each resource reconciliation commits."
    )
    @PostMapping("/update-assigned-resources-usage")
    @ResponseStatus(HttpStatus.OK)
    public void reconcileAssignedResourcesUsage() {
        log.info("Starting assigned resource usage reconciliation");
        updateAssignedResourcesService.updateAssignedResources();
    }

    private void syncAllFlattenedAssignments(boolean publishPendingMemberships) {
        log.info("Starting to sync all device assignments. publishPendingMemberships={}", publishPendingMemberships);

        List<Assignment> allAssignments = deviceAssignmentService.getAllActiveAssignments();
        allAssignments.forEach(assignment ->
                flattenedDeviceAssignmentService.syncFlattenedAssignments(assignment, publishPendingMemberships));
    }

}
