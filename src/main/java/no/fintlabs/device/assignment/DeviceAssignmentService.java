package no.fintlabs.device.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceAssignmentService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResourceRepository resourceRepository;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final ApplicationResourceLocationService applicationResourceLocationService;
    private final OpaService opaService;
    private final LicenseEnforcementService licenseEnforcementService;

    public Assignment createNewAssignment(Long resourceRef, String organizationUnitId, Long deviceGroupRef) {
        log.info("Trying to create new assignment for resource {} and device group {}", resourceRef, deviceGroupRef);

        DeviceGroup deviceGroup = getDeviceGroupOrThrow(deviceGroupRef);
        Resource resource = getResourceOrThrow(resourceRef);

        // Ensure no duplicate assignment
        assertNoExistingActiveAssignment(resourceRef, deviceGroupRef);


        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(resourceRef)
                .organizationUnitId(organizationUnitId)
                .resourceName(resource.getResourceName())
                .assignmentId(resourceRef + "_deviceGroup_" + LocalDateTime.now())
                .azureAdGroupId(resource.getIdentityProviderGroupObjectId())
                .deviceGroupRef(deviceGroup.getId())
                .build();

        Optional<NearestResourceLocationDto> nearestApplicationResourceLocationDto = applicationResourceLocationService
                .getNearestApplicationResourceLocationForOrgUnit(resourceRef, assignment.getOrganizationUnitId());

        nearestApplicationResourceLocationDto.ifPresent(nearestApplicationResourceLocation -> {
            assignment.setApplicationResourceLocationOrgUnitId(nearestApplicationResourceLocation.orgUnitId());
            assignment.setApplicationResourceLocationOrgUnitName(nearestApplicationResourceLocation.orgUnitName());
        });
        boolean updatedResourceLicenseCount = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignment);
        log.info("Incremented license count for resource {} : {}",
                assignment.getResourceRef(), updatedResourceLicenseCount ? "Success" : "Failure");
        if(!updatedResourceLicenseCount) {
            throw new ConflictException("Can't update number of assigned licenses for resource " + assignment.getResourceRef());
        }
        log.info("Saving assignment {}", assignment);
        Assignment newAssignment = assignmentRepository.saveAndFlush(assignment);
        log.info("Saved assignment {}", newAssignment);

        return newAssignment;
    }

    private DeviceGroup getDeviceGroupOrThrow(Long id) {
        return deviceGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device group not found: " + id));
    }

    private Resource getResourceOrThrow(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));

        if (resource.getIdentityProviderGroupObjectId() == null) {
            throw new AssignmentException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Resource " + id + " does not have azure group id set"
            );
        }

        return resource;
    }

    private void assertNoExistingActiveAssignment(Long resourceRef, Long deviceGroupRef) {
        if (existingDeviceGroupAssignment(deviceGroupRef, resourceRef)) {
            throw new ConflictException(
                    "Active assignment already exists for resource " + resourceRef + " and device group " + deviceGroupRef
            );
        }
    }

    public List<Assignment> getAllActiveAssignments() {
        return assignmentRepository.findAllByDeviceGroupRefIsNotNullAndAssignmentRemovedDateIsNull();
    }

    public void deleteAssignment(Long id) {
        log.info("Deleting assignment with id {}", id);

        String userName = opaService.getUserNameAuthenticatedUser();

        Assignment assignment = assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
        assignment.setAssignmentRemovedDate(new Date());

        if (!userName.isEmpty()) {
            userRepository.getUserByUserName(userName).ifPresent(user -> assignment.setAssignerRemoveRef(user.getId()));
        }

        assignmentRepository.saveAndFlush(assignment);
        licenseEnforcementService.decreaseAssignedResourcesWhenAssignmentRemoved(assignment);

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, "Associated assignment terminated by user");

    }

    private boolean existingDeviceGroupAssignment(Long deviceGroupRef, Long resourceRef) {
        return assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(deviceGroupRef, resourceRef).isPresent();
    }

    public List<Assignment> getActiveAssignmentsByResource(Long resourceId) {
        return assignmentRepository.findActiveDeviceAssignmentsByResourceRef(resourceId);
    }

    public Optional<Assignment> getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }
}
