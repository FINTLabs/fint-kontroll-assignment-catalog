package no.fintlabs.assignment.device.projection;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.device.*;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipRepository;
import no.fintlabs.assignment.device.entra.DeviceEntraMembershipPublisher;
import no.fintlabs.device.groupmembership.DeviceGroupMembership;
import no.fintlabs.device.groupmembership.DeviceGroupMembershipRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.enforcement.LicenseEnforcementService;
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
    private final DeviceEntraMembershipPublisher deviceAssigmentEntityProducerService;
    private final EntityManager entityManager;
    private final DeviceEntraMembershipRepository deviceEntraMembershipRepository;
    private final AssignmentRepository assignmentRepository;
    private final DeviceRepository deviceRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    @Async
    public void createAndPublishFlattenedAssignments(Assignment assignment) {
        createAndPublishFlattenedAssignmentsSync(assignment);
    }

    public void createAndPublishFlattenedAssignmentsSync(Assignment assignment) {
        Set<FlattenedDeviceAssignment> newAssignments = createFlattenedAssignments(assignment);
        saveAndPublishFlattenedAssignmentsBatch(new ArrayList<>(newAssignments));
    }

    public void updateApplicationResourceLocation(Assignment assignment) {
        List<FlattenedDeviceAssignment> flattenedAssignments =
                flattenedDeviceAssignmentRepository.findByAssignmentIdAndTerminationDateIsNull(assignment.getId());

        flattenedAssignments.forEach(flattenedAssignment ->
                flattenedAssignment.setApplicationResourceLocationOrgUnitId(
                        assignment.getApplicationResourceLocationOrgUnitId()
                )
        );
        saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, false);
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
        UUID resourceEntraId = assignment.getEntraGroupId();

        FlattenedDeviceAssignment flattenedAssignment = toFlattenedDeviceAssignment(assignment);
        flattenedAssignment.setIdentityProviderDeviceObjectId(deviceEntraId);
        flattenedAssignment.setDeviceRef(device.getId());

        DeviceEntraMembership deviceEntraMembership = deviceEntraMembershipRepository
                .findByDeviceEntraIdAndResourceEntraId(deviceEntraId, resourceEntraId)
                .map(this::getEntraMembershipForNewFlattenedAssignment)
                .orElseGet(() -> DeviceEntraMembership.builder()
                        .deviceEntraId(deviceEntraId)
                        .resourceEntraId(resourceEntraId)
                        .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
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
                        EntraMembershipSyncStatus.inactiveStatuses().contains(info.getEntraStatus());

        if (needsReset) {
            info.setEntraStatus(EntraMembershipSyncStatus.NOT_SENT);
            info.setMembershipStatus(MembershipStatus.ACTIVE);
            info.setSentToEntraAt(null);
            info.setDeletionSentToEntraAt(null);
        }

        return info;
    }


    public void saveAndPublishFlattenedAssignmentsBatch(List<FlattenedDeviceAssignment> flattenedAssignmentsForUpdate) {
        saveAndPublishFlattenedAssignmentsBatch(flattenedAssignmentsForUpdate, true);
    }

    public void saveAndPublishFlattenedAssignmentsBatch(
            List<FlattenedDeviceAssignment> flattenedAssignmentsForUpdate,
            boolean publishPendingMemberships
    ) {
        if (flattenedAssignmentsForUpdate == null || flattenedAssignmentsForUpdate.isEmpty()) {
            log.info("saveAndPublish - 0 flattened assignments, nothing to do");
            return;
        }

        log.info("saveAndPublish - {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;
        Set<Long> affectedResourceRefs = flattenedAssignmentsForUpdate.stream()
                .map(FlattenedDeviceAssignment::getResourceRef)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedDeviceAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);

            List<FlattenedDeviceAssignment> savedFlattened = flattenedDeviceAssignmentRepository.saveAll(batch);
            savedFlattened.forEach(flattenedAssignment ->
                    log.info("saveAndPublish - Flattened assignment with id: {}, assignmentId: {}",
                            flattenedAssignment.getId(), flattenedAssignment.getAssignmentId())
            );

            if (publishPendingMemberships) {
                savedFlattened.stream()
                        .map(FlattenedDeviceAssignment::getDeviceEntraMembership)
                        .filter(deviceEntraMembership -> deviceEntraMembership.getEntraStatus().equals(EntraMembershipSyncStatus.NOT_SENT))
                        .distinct()
                        .forEach(membership -> deviceAssigmentEntityProducerService.publish(membership, false));
            }

            flattenedDeviceAssignmentRepository.flush();
            entityManager.clear();
        }

        recalculateAssignedResources(affectedResourceRefs);
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
        recalculateAssignedResources(activeFlattenedAssignments);
    }

    public void publishDeactivatedFlattenedAssignmentsForDeletion(List<FlattenedDeviceAssignment> flattenedDeviceAssignments) {
        Set<DeviceEntraMembership> memberships = flattenedDeviceAssignments.stream()
                .map(FlattenedDeviceAssignment::getDeviceEntraMembership)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        memberships.forEach(deviceEntraMembership -> {
            boolean hasActiveAssignments =
                    flattenedDeviceAssignmentRepository.existsByDeviceEntraMembershipAndTerminationDateIsNull(deviceEntraMembership);
            if (shouldPublishDeviceMembershipDeletion(deviceEntraMembership, hasActiveAssignments)) {
                deviceEntraMembership.setMembershipStatus(MembershipStatus.INACTIVE);
                deviceAssigmentEntityProducerService.publish(deviceEntraMembership, true);
            }
        });
    }

    private boolean shouldPublishDeviceMembershipDeletion(
            DeviceEntraMembership deviceEntraMembership,
            boolean hasActiveAssignments
    ) {
        return !hasActiveAssignments
                && (EntraMembershipSyncStatus.activeStatuses().contains(deviceEntraMembership.getEntraStatus())
                || EntraMembershipSyncStatus.ERROR.equals(deviceEntraMembership.getEntraStatus()));
    }


    @Async
    public void syncFlattenedAssignments(Assignment assignment) {
        syncFlattenedAssignments(assignment, true);
    }

    @Async
    public void syncFlattenedAssignments(Assignment assignment, boolean publishPendingMemberships) {
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
            saveAndPublishFlattenedAssignmentsBatch(new ArrayList<>(toCreate), publishPendingMemberships);
        }

        if (!toTerminate.isEmpty()) {
            flattenedDeviceAssignmentRepository.saveAll(toTerminate);
            entityManager.flush();
            publishDeactivatedFlattenedAssignmentsForDeletion(toTerminate);
            recalculateAssignedResources(toTerminate);
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
        recalculateAssignedResources(activeAssignments);
    }

    public void deactivateAndDeleteAssignmentsForDevice(Long deviceId) {
        List<FlattenedDeviceAssignment> activeAssignments =
                flattenedDeviceAssignmentRepository.findByDeviceRefAndTerminationDateIsNull(deviceId);

        activeAssignments.forEach(assignment -> {
            assignment.setTerminationDate(new Date());
            assignment.setTerminationReason("Device deleted");
        });

        if (!activeAssignments.isEmpty()) {
            flattenedDeviceAssignmentRepository.saveAll(activeAssignments);
            entityManager.flush();
            publishDeactivatedFlattenedAssignmentsForDeletion(activeAssignments);
        }

        flattenedDeviceAssignmentRepository.deleteAllByDeviceRef(deviceId);
        flattenedDeviceAssignmentRepository.flush();
        recalculateAssignedResources(activeAssignments);
    }

    public void addAssignmentsForMembership(DeviceGroupMembership deviceGroupMembership) {
        if (!deviceGroupMembership.isActive()) {
            log.info("Device group membership for device {} and group {} is not active. No flattened assignments will be added",
                    deviceGroupMembership.getDeviceId(), deviceGroupMembership.getDeviceGroupId());
            return;
        }
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

        Set<DeviceEntraMembership> membershipsToPublish = new LinkedHashSet<>();
        for (FlattenedDeviceAssignment flattenedDeviceAssignment : assignments) {
            flattenedDeviceAssignment.setIdentityProviderDeviceObjectId(entraDeviceId);
            DeviceEntraMembership deviceEntraMembership = flattenedDeviceAssignment.getDeviceEntraMembership();
            deviceEntraMembership.setDeviceEntraId(entraDeviceId);
            membershipsToPublish.add(deviceEntraMembership);
        }

        flattenedDeviceAssignmentRepository.saveAll(assignments);
        deviceEntraMembershipRepository.saveAll(membershipsToPublish);

        log.info("Updated device entra id for {} flattened assignments", assignments.size());
        publishAfterCommit(membershipsToPublish);
    }

    private void publishAfterCommit(Set<DeviceEntraMembership> membershipsToPublish) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishDeviceMemberships(membershipsToPublish);
                }
            });
        } else {
            publishDeviceMemberships(membershipsToPublish);
        }
    }

    private void recalculateAssignedResources(Collection<FlattenedDeviceAssignment> flattenedAssignments) {
        flattenedAssignments.stream()
                .map(FlattenedDeviceAssignment::getResourceRef)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::recalculateAssignedResources);
    }

    private void recalculateAssignedResources(Set<Long> resourceRefs) {
        resourceRefs.forEach(this::recalculateAssignedResources);
    }

    private void recalculateAssignedResources(Long resourceRef) {
        log.info("Recalculating assigned resources for resource {}", resourceRef);
        licenseEnforcementService.recalculateAssignedResourcesForResource(resourceRef);
    }

    private void publishDeviceMemberships(Set<DeviceEntraMembership> membershipsToPublish) {
        membershipsToPublish.forEach(membership ->
                deviceAssigmentEntityProducerService.publish(
                        membership,
                        EntraMembershipSyncStatus.ERROR.equals(membership.getEntraStatus())
                )
        );
    }
}
