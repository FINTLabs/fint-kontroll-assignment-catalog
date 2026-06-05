package no.fintlabs.device.assignment;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.*;
import no.fintlabs.device.entra.DeviceEntraMembership;
import no.fintlabs.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
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
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
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
        log.info("Preparing {} memberships from deviceGroup {} for new assignment {} for flattened assignment creation",
                memberships.size(),
                assignment.getDeviceGroupRef(),
                assignment.getId());

        Set<FlattenedDeviceAssignment> flattenedAssignments = new HashSet<>();

        for (DeviceGroupMembership membership : memberships) {
            flattenedAssignments.add(mapToFlattenedAssignment(membership, assignment));
        }

        return flattenedAssignments;
    }

    private FlattenedDeviceAssignment mapToFlattenedAssignment(DeviceGroupMembership membership,
                                                               Assignment assignment) {

        Device device = getDeviceOrThrow(membership.getDeviceId());
        UUID deviceEntraId = device.getDataObjectId();
        UUID resourceEntraId = assignment.getEntraIdGroupId();

        FlattenedDeviceAssignment flattenedAssignment = toFlattenedDeviceAssignment(assignment);
        flattenedAssignment.setIdentityProviderDeviceObjectId(deviceEntraId);
        flattenedAssignment.setDeviceRef(device.getId());

        DeviceEntraMembership deviceEntraMembership = deviceEntraMembershipRepository
                .findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId)
                .map(this::getEntraMembershipForNewFlattenedAssignment)
                .orElseGet(() -> DeviceEntraMembership.builder()
                        .deviceEntraId(deviceEntraId)
                        .resourceEntraId(resourceEntraId)
                        .entraStatus(EntraStatus.NOT_SENT)
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .build()
                );

        deviceEntraMembership.addFlattenedAssignment(flattenedAssignment);

        return flattenedAssignment;
    }
    private Device getDeviceOrThrow(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalStateException(
                        "Device not found in database with id: " + deviceId +
                                ". This indicates inconsistent data or a cascade delete issue."
                ));
    }

    private DeviceEntraMembership getEntraMembershipForNewFlattenedAssignment(DeviceEntraMembership info) {
        boolean needsReset =
                info.getMembershipStatus() == MembershipStatus.INACTIVE ||
                        EntraStatus.inactiveStatuses().contains(info.getEntraStatus());

        if (needsReset) {
            info.setEntraStatus(EntraStatus.NOT_SENT);
            info.setMembershipStatus(MembershipStatus.ACTIVE);
            info.setSentToEntraAt(null);
            info.setDeletionSentToEntraAt(null);
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

            savedFlattened.stream().map(FlattenedDeviceAssignment::getDeviceEntraMembership).filter(deviceEntraMembership -> deviceEntraMembership.getEntraStatus().equals(EntraStatus.NOT_SENT))
                    .distinct().forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, false));

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
            DeviceEntraMembership deviceEntraMembership = flattenedAssignment.getDeviceEntraMembership();
            if ((EntraStatus.activeStatuses().contains(deviceEntraMembership.getEntraStatus()) || EntraStatus.ERROR.equals(deviceEntraMembership.getEntraStatus())) && deviceEntraMembership.getFlattenedDeviceAssignments().isEmpty()) {
                deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);
                deviceAssigmentEntityProducerService.publish(deviceEntraMembership, true);
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
                        (a, b) -> a
                ));

        List<FlattenedDeviceAssignment> toTerminate = new ArrayList<>();
        List<FlattenedDeviceAssignment> toCreate = new ArrayList<>();


        for (FlattenedDeviceAssignment existing : existingActiveAssignments) {
            Long deviceRef = existing.getDeviceRef();
            if (!requiredByDeviceRef.containsKey(deviceRef)) {
                existing.setTerminationDate(new Date());
                existing.setTerminationReason("Assignment sync – flattened assignment no longer valid");
                toTerminate.add(existing);
            }
        }


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

    public void updateDeviceEntraId(Device device) {
        UUID entraDeviceId = device.getDataObjectId();
        List<FlattenedDeviceAssignment> assignments = flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(device.getId());
        if (assignments.isEmpty()) {
            log.info("No active flattened assignments for device {}", device.getId());
            return;
        }

        for (FlattenedDeviceAssignment flattenedDeviceAssignment : assignments) {
            flattenedDeviceAssignment.setIdentityProviderDeviceObjectId(entraDeviceId);
            flattenedDeviceAssignment.getDeviceEntraMembership().setDeviceEntraId(entraDeviceId);
        }

        log.info("Updated device entra id for {} flattened assignments", assignments.size());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (FlattenedDeviceAssignment assignment : assignments) {
                    deviceAssigmentEntityProducerService.publish(assignment.getDeviceEntraMembership(), false);
                }
            }
        });
    }
}
