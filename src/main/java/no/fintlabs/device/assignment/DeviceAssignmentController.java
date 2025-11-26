package no.fintlabs.device.assignment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.SimpleAssignment;
import no.fintlabs.assignment.exception.AssignmentAlreadyExistsException;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.util.OnlyDevelopers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/device-assignments")
@RequiredArgsConstructor
public class DeviceAssignmentController {

    private final DeviceAssignmentService deviceAssignmentService;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final UpdateAssignedResourcesService updateAssignedResourcesService;


    @PostMapping
    public ResponseEntity<SimpleAssignment> createAssignment(@Valid @RequestBody NewDeviceAssignmentRequest request) {
        log.info("Creating assignment. Request - deviceGroupRef: {}, resourceRef: {}, organizationUnitId: {}", request.deviceGroupRef, request.resourceRef, request.organizationUnitId);

        try {
            Assignment newAssignment =
                    deviceAssignmentService.createNewAssignment(request.resourceRef, request.organizationUnitId, request.deviceGroupRef);
            flattenedDeviceAssignmentService.createAndPublishFlattenedAssignments(newAssignment);
            return new ResponseEntity<>(newAssignment.toSimpleAssignment(), HttpStatus.CREATED);
        } catch (AssignmentAlreadyExistsException exception) {
            throw new ConflictException("Assignment already exists");
        }
    }

    @PostMapping("/syncflattenedassignments")
    public void syncFlattenedAssignments() {
        log.info("Starting to sync all flattened assignments");
        List<Assignment> allAssignments = deviceAssignmentService.getAllActiveAssignments();
        allAssignments.forEach(flattenedDeviceAssignmentService::syncFlattenedAssignments);
    }

    @PostMapping("/syncflattenedassignments/{assignmentId}")
    public void syncFlattenedAssignmentById(@PathVariable("assignmentId") Long assignmentId) {
        log.info("Starting to sync flattened assignments for assignment {}", assignmentId);

        deviceAssignmentService.getAssignmentById(assignmentId)
                .ifPresent(flattenedDeviceAssignmentService::syncFlattenedAssignments);
    }

    @PostMapping("/syncflattenedassignments/resource/{id}")
    public void syncFlattenedAssignmentsByResourceId(@PathVariable("id") Long resourceId) {
        log.info("Starting to sync assignments for resource: {}", resourceId);

        deviceAssignmentService.getActiveAssignmentsByResource(resourceId)
                .forEach(flattenedDeviceAssignmentService::syncFlattenedAssignments);

        log.info("Started syncing all flattened assignments for resource: {}", resourceId);
    }

    @DeleteMapping("{id}")
    public void deleteAssignment(@PathVariable("id") Long id) {
        deviceAssignmentService.deleteAssignment(id);
    }

    @PostMapping("/update-assigned-resources-usage")
    // runs for both user and device assignments
    public void updateAssignedResourcesUsage() {
        log.info("Start updating resources usage for device assignments");
        updateAssignedResourcesService.updateAssignedResources();
    }

}

