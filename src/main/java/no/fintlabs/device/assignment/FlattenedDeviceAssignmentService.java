package no.fintlabs.device.assignment;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.*;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
import no.fintlabs.device.azureInfo.DeviceAzureInfoRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedDeviceAssignment;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlattenedDeviceAssignmentService {

    private final FlattenedDeviceAssignmentRepository flattenedDeviceAssignmentRepository;
    private final DeviceGroupMembershipRepository deviceGroupMembershipRepository;
    private final DeviceAssigmentEntityProducerService deviceAssigmentEntityProducerService;
    private final EntityManager entityManager;
    private final DeviceAzureInfoRepository deviceAzureInfoRepository;
    private final AssignmentRepository assignmentRepository;
    private final DeviceRepository deviceRepository;

    @Async
    public void createAndPublishFlattenedAssignments(Assignment assignment) {
        Set<FlattenedDeviceAssignment> newAssignments = createFlattenedAssignments(assignment);
        saveAndPublishFlattenedAssignmentsBatch(new ArrayList<>(newAssignments));
    }

    public Set<FlattenedDeviceAssignment> createFlattenedAssignments(Assignment assignment) {
        log.info("Creating flattened assignments for new device group assignment with id: {}", assignment.getId());

        List<DeviceGroupMembership> activeMemberships =
                deviceGroupMembershipRepository.findAllActiveByDeviceGroupRef(assignment.getDeviceGroupRef());

        if (activeMemberships.isEmpty()) {
            log.warn("Device group has no active memberships. No flattened assignment saved. Device group id: {}",
                    assignment.getDeviceGroupRef());
            return Set.of();
        }
        return mapNewAssignmentToFlattenedAssignments(activeMemberships, assignment);
    }


    private Set<FlattenedDeviceAssignment> mapNewAssignmentToFlattenedAssignments(
            List<DeviceGroupMembership> memberships,
            Assignment assignment
    ) {
        log.info("Preparing {} memberships from role {} for new assignment {} for flattened assignment creation",
                memberships.size(),
                assignment.getRoleRef(),
                assignment.getId());

        Set<FlattenedDeviceAssignment> flattenedAssignments = new HashSet<>();

        for (DeviceGroupMembership membership : memberships) {
            flattenedAssignments.add(mapToFlattenedAssignment(membership, assignment));
        }

        return flattenedAssignments;
    }

    private FlattenedDeviceAssignment mapToFlattenedAssignment(DeviceGroupMembership membership,
                                                               Assignment assignment) {

        UUID deviceAzureId = membership.getDevice().getDataObjectId();
        UUID resourceAzureId = assignment.getAzureAdGroupId();

        FlattenedDeviceAssignment flattenedAssignment = toFlattenedDeviceAssignment(assignment);
        flattenedAssignment.setIdentityProviderDeviceObjectId(deviceAzureId);
        flattenedAssignment.setDeviceRef(membership.getDevice().getId());

        DeviceAzureInfo azureInfo = deviceAzureInfoRepository
                .findByDeviceAzureIdAndResourceAzureId(deviceAzureId, resourceAzureId)
                .map(this::getAzureInfoForNewFlattenedAssignment)
                .orElseGet(() -> DeviceAzureInfo.builder()
                        .deviceAzureId(deviceAzureId)
                        .resourceAzureId(resourceAzureId)
                        .azureStatus(AzureStatus.NOT_SENT)
                        .kontrollStatus(KontrollStatus.ACTIVE)
                        .build()
                );

        azureInfo.addFlattenedAssignment(flattenedAssignment);

        return flattenedAssignment;
    }

    private DeviceAzureInfo getAzureInfoForNewFlattenedAssignment(DeviceAzureInfo info) {
        boolean needsReset =
                info.getKontrollStatus() == KontrollStatus.INACTIVE ||
                        AzureStatus.inactiveStatuses().contains(info.getAzureStatus());

        if (needsReset) {
            info.setAzureStatus(AzureStatus.NOT_SENT);
            info.setKontrollStatus(KontrollStatus.ACTIVE);
            info.setSentToAzureAt(null);
            info.setDeletionSentToAzureAt(null);
        }

        return info;
    }


    public void saveAndPublishFlattenedAssignmentsBatch(List<FlattenedDeviceAssignment> flattenedAssignmentsForUpdate) {
        if (flattenedAssignmentsForUpdate == null || flattenedAssignmentsForUpdate.isEmpty()) {
            log.info("saveAndPublish - 0 flattened assignments, nothing to do");
            return;
        }

        log.info("saveAndPublish - {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedDeviceAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);

            List<FlattenedDeviceAssignment> savedFlattened = flattenedDeviceAssignmentRepository.saveAll(batch);
            savedFlattened.forEach(flattenedAssignment ->
                    log.info("saveAndPublish - Flattened assignment with id: {}, assignmentId: {}",
                            flattenedAssignment.getId(), flattenedAssignment.getAssignmentId())
            );

            log.info("saveAndPublish - Publishing {} new flattened assignments to azure", savedFlattened.size());
            savedFlattened.stream().map(FlattenedDeviceAssignment::getAzureInfo).filter(deviceAzureInfo -> deviceAzureInfo.getAzureStatus().equals(AzureStatus.NOT_SENT))
                    .distinct().forEach(deviceAssigmentEntityProducerService::publish);

            flattenedDeviceAssignmentRepository.flush();
            entityManager.clear();
        }

        log.info("saveAndPublish - Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    @Transactional
    public void deleteFlattenedDeviceAssignments(Assignment assignment, String deactivationReason) {
        log.info("Deactivate flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedDeviceAssignment> activeFlattenedAssignments =
                flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(assignment.getId());

        for (FlattenedDeviceAssignment fda : activeFlattenedAssignments) {
            fda.setTerminationReason(deactivationReason);
            fda.setTerminationDate(assignment.getAssignmentRemovedDate());
        }

        flattenedDeviceAssignmentRepository.saveAll(activeFlattenedAssignments);
        entityManager.flush();
        publishDeactivatedFlattenedAssignmentsForDeletion(activeFlattenedAssignments);
    }

    public void publishDeactivatedFlattenedAssignmentsForDeletion(List<FlattenedDeviceAssignment> flattenedDeviceAssignments) {
        flattenedDeviceAssignments.forEach(flattenedAssignment -> {
            DeviceAzureInfo deviceAzureInfo = flattenedAssignment.getAzureInfo();
            if (AzureStatus.activeStatuses().contains(deviceAzureInfo.getAzureStatus()) && deviceAzureInfo.getFlattenedDeviceAssignments().isEmpty()) {
                deviceAzureInfo.setKontrollStatus(KontrollStatus.INACTIVE);
                deviceAssigmentEntityProducerService.publish(deviceAzureInfo);
            }
        });
    }


    @Async
    public void syncFlattenedAssignments(Assignment assignment) {
        Long assignmentId = assignment.getId();
        log.info("Updating flattened assignments for assignment with id {}", assignmentId);

        List<FlattenedDeviceAssignment> existingActiveAssignments =
                flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(assignmentId);

        Set<FlattenedDeviceAssignment> requiredFlattenedAssignments =
                createFlattenedAssignments(assignment);


        Map<Long, FlattenedDeviceAssignment> existingByDeviceRef = existingActiveAssignments.stream()
                .collect(Collectors.toMap(FlattenedDeviceAssignment::getDeviceRef, Function.identity()));

        Map<Long, FlattenedDeviceAssignment> requiredByDeviceRef = requiredFlattenedAssignments.stream()
                .collect(Collectors.toMap(
                        FlattenedDeviceAssignment::getDeviceRef,
                        Function.identity(),
                        (a, b) -> a // in case of duplicates, keep first
                ));

        List<FlattenedDeviceAssignment> toTerminate = new ArrayList<>();
        List<FlattenedDeviceAssignment> toCreate = new ArrayList<>();

        // Determine which existing assignments should be terminated
        for (FlattenedDeviceAssignment existing : existingActiveAssignments) {
            Long deviceRef = existing.getDeviceRef();
            if (!requiredByDeviceRef.containsKey(deviceRef)) {
                existing.setTerminationDate(new Date());
                existing.setTerminationReason("Assignment sync – flattened assignment no longer valid");
                toTerminate.add(existing);
            }
        }

        // Determine which new assignments must be created
        for (FlattenedDeviceAssignment required : requiredFlattenedAssignments) {
            Long deviceRef = required.getDeviceRef();
            if (!existingByDeviceRef.containsKey(deviceRef)) {
                toCreate.add(required);
            }
        }

        if (!toCreate.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(new ArrayList<>(toCreate));
        }

        if (!toTerminate.isEmpty()) {
            flattenedDeviceAssignmentRepository.saveAll(toTerminate);
            publishDeactivatedFlattenedAssignmentsForDeletion(toTerminate);
        }

        log.info(
                "Found {} active flattened assignments for assignment {}, and {} should be active",
                existingActiveAssignments.size(),
                assignmentId,
                requiredFlattenedAssignments.size()
        );
    }


    public void deactivateAssignmentsForMembership(Long deviceId, Long deviceGroupId) {
        List<FlattenedDeviceAssignment> activeAssignments = flattenedDeviceAssignmentRepository.findByDeviceRefAndAssignmentViaGroupRefAndTerminationDateIsNull(deviceId, deviceGroupId);
        activeAssignments.forEach(assignment ->
        {
            assignment.setTerminationDate(new Date());
            assignment.setTerminationReason("Role membership deactivated");
        });
        flattenedDeviceAssignmentRepository.saveAll(activeAssignments);
        entityManager.flush();
        publishDeactivatedFlattenedAssignmentsForDeletion(activeAssignments);
    }

    public void addAssignmentsForMembership(DeviceGroupMembership deviceGroupMembership) {
        List<Assignment> activeGroupAssignments = assignmentRepository.findAssignmentsByDeviceGroupRefAndAssignmentRemovedDateIsNull(deviceGroupMembership.getDeviceGroupId());
        List<FlattenedDeviceAssignment> newAssignments = activeGroupAssignments.stream().map(assignment -> mapToFlattenedAssignment(deviceGroupMembership, assignment)).toList();
        saveAndPublishFlattenedAssignmentsBatch(newAssignments);
    }

    public void updateDeviceAzureId(Device device) {
        UUID azureDeviceId = device.getDataObjectId();
        List<FlattenedDeviceAssignment> assignments = flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(device.getId());
        if (assignments.isEmpty()) {
            log.info("No active flattened assignments for device {}", device.getId());
            return;
        }

        for (FlattenedDeviceAssignment flattenedDeviceAssignment : assignments) {
            flattenedDeviceAssignment.setIdentityProviderDeviceObjectId(azureDeviceId);
            flattenedDeviceAssignment.getAzureInfo().setDeviceAzureId(azureDeviceId);
        }

        log.info("Updated device azure id for {} flattened assignments", assignments.size());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (FlattenedDeviceAssignment assignment : assignments) {
                    deviceAssigmentEntityProducerService.publish(assignment.getAzureInfo());
                }
            }
        });
    }
}
