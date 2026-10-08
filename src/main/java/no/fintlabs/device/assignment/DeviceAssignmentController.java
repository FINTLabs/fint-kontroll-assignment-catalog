package no.fintlabs.device.assignment;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.util.OnlyDevelopers;
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

    private final DeviceAssignmentService deviceAssignmentService;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final UpdateAssignedResourcesService updateAssignedResourcesService;

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignments")
    public void syncFlattenedAssignments() {
        log.info("Starting to sync all flattened assignments");
        List<Assignment> allAssignments = deviceAssignmentService.getAllActiveAssignments();
        allAssignments.forEach(flattenedDeviceAssignmentService::syncFlattenedAssignments);
    }

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignments/{assignmentId}")
    public void syncFlattenedAssignmentById(@PathVariable("assignmentId") Long assignmentId) {
        log.info("Starting to sync flattened assignments for assignment {}", assignmentId);

        deviceAssignmentService.getAssignmentById(assignmentId)
                .ifPresent(flattenedDeviceAssignmentService::syncFlattenedAssignments);
    }

    @OnlyDevelopers
    @PostMapping("/syncflattenedassignments/resource/{id}")
    public void syncFlattenedAssignmentsByResourceId(@PathVariable("id") Long resourceId) {
        log.info("Starting to sync assignments for resource: {}", resourceId);

        deviceAssignmentService.getActiveAssignmentsByResource(resourceId)
                .forEach(flattenedDeviceAssignmentService::syncFlattenedAssignments);

        log.info("Started syncing all flattened assignments for resource: {}", resourceId);
    }

    @PostMapping("/update-assigned-resources-usage")
    @OnlyDevelopers
    // runs for both user and device assignments
    public void updateAssignedResourcesUsage() {
        log.info("Start updating resources usage for device assignments");
        updateAssignedResourcesService.updateAssignedResources();
    }

}
