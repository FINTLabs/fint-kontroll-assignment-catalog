package no.fintlabs.assignment.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupAssignment;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.device.group.DeviceGroupSpecificationBuilder;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.exception.ConflictException;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.AssignmentResource;
import no.fintlabs.resource.DeviceGroupResourceSpecificationBuilder;
import no.fintlabs.resource.Resource;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.resource.ResourceAssignmentValidator;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceAssignmentService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentValidator resourceAssignmentValidator;
    private final FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;
    private final ApplicationResourceLocationService applicationResourceLocationService;
    private final OpaService opaService;
    private final LicenseEnforcementService licenseEnforcementService;

    @Transactional
    public Assignment createNewAssignment(Long resourceRef, String organizationUnitId, Long deviceGroupRef) {
        log.info("Trying to create new assignment for resource {} and device group {}", resourceRef, deviceGroupRef);

        DeviceGroup deviceGroup = getDeviceGroupOrThrow(deviceGroupRef);
        Resource resource = resourceAssignmentValidator.getActiveResourceOrThrow(resourceRef);

        // Ensure no duplicate assignment
        assertNoExistingActiveAssignment(resourceRef, deviceGroupRef);

        Assignment assignment = Assignment.builder()
                .assignerUserName(opaService.getUserNameAuthenticatedUser())
                .resourceRef(resourceRef)
                .organizationUnitId(organizationUnitId)
                .resourceName(resource.getResourceName())
                .assignmentId(String.format(
                        "%s_deviceGroup_%s_%s",
                        resourceRef,
                        deviceGroup.getId(),
                        LocalDateTime.now()
                ))
                .entraGroupId(resource.getIdentityProviderGroupObjectId())
                .deviceGroupRef(deviceGroup.getId())
                .build();

        Optional<NearestResourceLocationDto> nearestApplicationResourceLocationDto = applicationResourceLocationService
                .getNearestApplicationResourceLocationForOrgUnit(resourceRef, assignment.getOrganizationUnitId());

        nearestApplicationResourceLocationDto.ifPresent(nearestApplicationResourceLocation -> {
            assignment.setApplicationResourceLocationOrgUnitId(nearestApplicationResourceLocation.orgUnitId());
            assignment.setApplicationResourceLocationOrgUnitName(nearestApplicationResourceLocation.orgUnitName());
        });
        boolean availableCapacity = licenseEnforcementService.validateAssignmentCapacity(assignment);
        log.info("Validated license capacity for resource {} : {}",
                assignment.getResourceRef(), availableCapacity ? "Success" : "Failure");
        if (!availableCapacity) {
            throw new ConflictException("Can't assign more licenses for resource " + assignment.getResourceRef());
        }
        log.info("Saving assignment {}", assignment);
        Assignment newAssignment = assignmentRepository.saveAndFlush(assignment);
        log.info("Saved assignment {}", newAssignment);

        flattenedDeviceAssignmentService.createAndPublishFlattenedAssignmentsSync(newAssignment);

        return newAssignment;
    }

    private DeviceGroup getDeviceGroupOrThrow(Long id) {
        return deviceGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device group not found: " + id));
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

    @Transactional
    public void deleteAssignment(Long id) {
        log.info("Deleting assignment with id {}", id);

        String userName = opaService.getUserNameAuthenticatedUser();

        Assignment assignment = assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
        validateDeviceAssignment(assignment);

        if (!userName.isEmpty()) {
            userRepository.getUserByUserName(userName).ifPresent(user -> assignment.setAssignerRemoveRef(user.getId()));
        }

        deactivateAssignment(assignment, "Associated assignment terminated by user");

    }

    @Transactional
    public void deactivateAssignmentsByResourceId(Long resourceId) {
        getActiveAssignmentsByResource(resourceId)
                .forEach(assignment -> deactivateAssignment(assignment, "Resource is no longer active"));
    }

    private void deactivateAssignment(Assignment assignment, String terminationReason) {
        assignment.setAssignmentRemovedDate(new Date());
        assignmentRepository.saveAndFlush(assignment);

        flattenedDeviceAssignmentService.deleteFlattenedDeviceAssignments(assignment, terminationReason);
        licenseEnforcementService.recalculateAssignedResources(assignment);
    }

    private void validateDeviceAssignment(Assignment assignment) {
        if (assignment.getDeviceGroupRef() == null) {
            throw new ResourceNotFoundException("Device assignment not found: " + assignment.getId());
        }
    }

    private boolean existingDeviceGroupAssignment(Long deviceGroupRef, Long resourceRef) {
        return assignmentRepository.findAssignmentsByDeviceGroupRefAndResourceRefAndAssignmentRemovedDateIsNull(deviceGroupRef, resourceRef).isPresent();
    }

    public List<Assignment> getActiveAssignmentsByResource(Long resourceId) {
        return assignmentRepository.findActiveDeviceAssignmentsByResourceRef(resourceId);
    }

    public Page<DeviceGroupAssignment> findDeviceGroupAssignmentsForResource(Long resourceId, String search, Pageable pageable) {
        List<Assignment> activeAssignments = getDistinctDeviceGroupAssignments(getActiveAssignmentsByResource(resourceId));

        if (activeAssignments.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, Assignment> assignmentsByDeviceGroupId = activeAssignments
                .stream()
                .collect(Collectors.toMap(Assignment::getDeviceGroupRef, Function.identity()));

        List<Long> deviceGroupIds = activeAssignments
                .stream()
                .map(Assignment::getDeviceGroupRef)
                .toList();

        return deviceGroupRepository.findAll(
                        new DeviceGroupSpecificationBuilder(deviceGroupIds, search).assignmentSearch(),
                        pageable
                )
                .map(deviceGroup -> toDeviceGroupAssignment(deviceGroup, assignmentsByDeviceGroupId.get(deviceGroup.getId())));
    }

    public List<Assignment> getActiveAssignmentsByDeviceGroup(Long deviceGroupId) {
        return assignmentRepository.findAssignmentsByDeviceGroupRefAndAssignmentRemovedDateIsNull(deviceGroupId);
    }

    public Page<AssignmentResource> findResourcesAssignedToDeviceGroup(
            Long deviceGroupId,
            String resourceType,
            String search,
            List<Long> resourceIds,
            Pageable pageable
    ) {
        return resourceRepository.findAll(
                        new DeviceGroupResourceSpecificationBuilder(deviceGroupId, resourceType, search, resourceIds).build(),
                        pageable
                )
                .map(Resource::toSimpleResource)
                .map(resource -> {
                    assignmentRepository
                            .findAssignmentsByDeviceGroupRefAndResourceRefAndAssignmentRemovedDateIsNull(deviceGroupId, resource.getId())
                            .ifPresent(assignment -> {
                                resource.setAssignmentRef(assignment.getId());
                                resource.setAssignerUsername(assignment.getAssignerUserName());
                                resource.setAssignerDisplayname(getAssignerDisplayname(assignment.getAssignerUserName()).orElse(null));
                            });
                    return resource;
                });
    }

    public Optional<Assignment> getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }

    public Optional<String> getAssignerDisplayname(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.getUserByUserName(username)
                .map(user -> {
                    String firstName = user.getFirstName();
                    String lastName = user.getLastName();
                    if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
                        return firstName + " " + lastName;
                    }
                    return null;
                });
    }

    private List<Assignment> getDistinctDeviceGroupAssignments(List<Assignment> assignments) {
        return assignments
                .stream()
                .filter(assignment -> assignment.getDeviceGroupRef() != null)
                .collect(Collectors.toMap(
                        Assignment::getDeviceGroupRef,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    private DeviceGroupAssignment toDeviceGroupAssignment(DeviceGroup deviceGroup, Assignment assignment) {
        if (assignment == null) {
            log.warn("Assignment not found for device group {}", deviceGroup.getId());
        }

        String assignerUsername = assignment == null ? null : assignment.getAssignerUserName();

        return DeviceGroupAssignment.builder()
                .id(deviceGroup.getId())
                .sourceId(deviceGroup.getSourceId())
                .name(deviceGroup.getName())
                .orgUnitId(deviceGroup.getOrgUnitId())
                .platform(deviceGroup.getPlatform())
                .deviceType(deviceGroup.getDeviceType())
                .createdDate(deviceGroup.getCreatedDate())
                .modifiedDate(deviceGroup.getModifiedDate())
                .noOfMembers(deviceGroup.getNoOfMembers())
                .assignmentRef(assignment == null ? null : assignment.getId())
                .organizationUnitId(assignment == null ? null : assignment.getOrganizationUnitId())
                .organisationUnitName(assignment == null ? null : assignment.getApplicationResourceLocationOrgUnitName())
                .assignerUsername(assignerUsername)
                .assignerDisplayname(getAssignerDisplayname(assignerUsername).orElse(null))
                .assignmentDate(assignment == null ? null : assignment.getAssignmentDate())
                .build();
    }
}
