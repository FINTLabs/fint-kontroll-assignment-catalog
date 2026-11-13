package no.fintlabs.assignment.user.projection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.membership.Membership;
import no.fintlabs.user.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlattenedAssignmentService {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    private final FlattenedAssignmentMapper flattenedAssignmentMapper;
    private final UserEntraMembershipRepository userEntraMembershipRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    private final UserEntraMembershipPublisher assigmentEntityProducerService;

    @Transactional
    public void createForNewAssignment(Assignment assignment) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot create or update flattened assignment");
            return;
        }

        log.info("Creating flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        if (assignment.isUserAssignment()) {
            flattenedAssignments.add(toFlattenedAssignment(assignment));
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.createFlattenedAssignmentsForNewRoleAssignment(assignment));
        } else {
            log.error("Assignment with id {} is not a user or group assignment, not creating flattened assignment", assignment.getId());
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, true);
        }
    }

    @Async
    @Transactional
    public void reconcileForAssignmentAsync(Assignment assignment, boolean publishPendingMemberships) {
        reconcileForAssignment(assignment, publishPendingMemberships);
    }

    @Transactional
    public void reconcileForAssignment(Assignment assignment, boolean publishPendingMemberships) {
        if (assignment.getId() == null) {
            log.error("Assignment id is null. Cannot update flattened assignment");
            return;
        }

        log.info("Updating flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        List<FlattenedAssignment> existingActiveAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());

        log.info("Found {} active flattened assignments for assignment {}", existingActiveAssignments.size(), assignment.getId());

        if (assignment.isUserAssignment()) {
            FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingActiveAssignments)
                    .ifPresent(flattenedAssignments::add);
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingActiveAssignments));
        } else {
            log.error("Assignment with id {} is not a user or group assignment, not updating any flattened assignment", assignment.getId());
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, publishPendingMemberships);
        }
    }

    @Transactional
    public void reconcileForRoleMembership(Assignment assignment, Membership membership) {
        Long userRef = membership.getMemberId();
        Long roleRef = membership.getRoleId();

        List<FlattenedAssignment> existingFlattenedAssignments =
                flattenedAssignmentRepository.findByAssignmentIdAndUserRefAndAssignmentViaRoleRefAndAssignmentTerminationDateIsNull(assignment.getId(), userRef, roleRef);

        if(existingFlattenedAssignments.isEmpty() && !membership.isActive()) {
            log.info("Membership is not active and no existing flattened assignment found for role {}, user {} and assignment {}. No new flattened assignment created", roleRef, userRef, assignment.getId());
            return;
        }
        if(existingFlattenedAssignments.isEmpty()) {
            log.info("No flattened assignment found for active membership - role {}, user {} and assignment {}. Creating new", roleRef, userRef, assignment.getId());
            FlattenedAssignment mappedFlattenedAssignment = toFlattenedAssignment(assignment);
            mappedFlattenedAssignment.setUserRef(userRef);
            mappedFlattenedAssignment.setAssignmentViaRoleRef(roleRef);
            mappedFlattenedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());

            saveAndPublishNewFlattenedAssignment(mappedFlattenedAssignment, true);
            return;
        }
        log.info("Found {} existing flattened assignments for role {}, user {} and assignment {}. Updating if needed",
                existingFlattenedAssignments.size(),
                roleRef,
                userRef,
                assignment.getId()
        );

        if (!membership.isActive()) {
            terminateFlattenedAssignments(existingFlattenedAssignments, "Role membership deactivated", new Date());
            return;
        }

        existingFlattenedAssignments.stream()
                .map(FlattenedAssignment::getUserEntraMembership)
                .filter(Objects::nonNull)
                .distinct()
                .map(userEntraMembership ->
                        migrateUserEntraMembership(userEntraMembership, membership.getIdentityProviderUserObjectId()))
                .flatMap(Optional::stream)
                .distinct()
                .forEach(userEntraMembership -> assigmentEntityProducerService.publish(userEntraMembership, false));
    }

    private void saveAndPublishNewFlattenedAssignment(FlattenedAssignment newFlattenedAssignment, boolean publishPendingMemberships) {
        log.info("Saving new flattened assignment for role {}, user {} and assignment {}",
                newFlattenedAssignment.getAssignmentViaRoleRef(),
                newFlattenedAssignment.getUserRef(),
                newFlattenedAssignment.getAssignmentId());

        linkUserEntraMembership(newFlattenedAssignment, new HashMap<>());
        FlattenedAssignment savedAssignment = flattenedAssignmentRepository.saveAndFlush(newFlattenedAssignment);

        int messagesScheduled = publishPendingMemberships
                ? publishUnsentMemberships(List.of(savedAssignment))
                : 0;
        recalculateAssignedResources(List.of(savedAssignment));

        log.info("Saved flattened assignment {} for assignment {}. Membership messages scheduled: {}",
                savedAssignment.getId(), savedAssignment.getAssignmentId(), messagesScheduled);
    }

    void saveAndPublishFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate, boolean publishPendingMemberships) {
        log.info("saveAndPublish - {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;
        Set<Long> affectedResourceRefs = new HashSet<>();
        Map<EntraMembershipKey, UserEntraMembership> membershipsByEntraId = new HashMap<>();
        int totalMembershipMessagesScheduled = 0;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            batch.forEach(flattenedAssignment -> linkUserEntraMembership(flattenedAssignment, membershipsByEntraId));
            List<FlattenedAssignment> savedFlattened = flattenedAssignmentRepository.saveAll(batch);
            savedFlattened.stream()
                    .map(FlattenedAssignment::getResourceRef)
                    .filter(Objects::nonNull)
                    .forEach(affectedResourceRefs::add);
            log.debug("saveAndPublish - Saved flattened assignment ids: {}",
                    savedFlattened.stream().map(FlattenedAssignment::getId).toList());

            if (publishPendingMemberships) {
                totalMembershipMessagesScheduled += publishUnsentMemberships(savedFlattened);
            }
        }

        flattenedAssignmentRepository.flush();
        recalculateAssignedResources(affectedResourceRefs);

        log.info("saveAndPublish - Saved {} flattened assignments. membershipMessagesScheduled={}, affectedResources={}",
                flattenedAssignmentsForUpdate.size(),
                totalMembershipMessagesScheduled,
                affectedResourceRefs.size());
    }

    @Transactional
    public void terminateForRemovedAssignment(Assignment assignment, String deactivationReason) {
        log.info("Deactivate flattened assignments for assignment with id {}", assignment.getId());

        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());
        terminateFlattenedAssignments(flattenedAssignments, deactivationReason, assignment.getAssignmentRemovedDate());
    }

    @Transactional
    public void terminateByIds(Set<Long> flattenedAssignmentIds) {
        log.info("Deactivate flattened assignments:  {}", flattenedAssignmentIds);

        String deactivationReason = "Role membership deactivated";
        Date deactivationDate = new Date();
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentIds
                .stream()
                .map(flattenedAssignmentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        terminateFlattenedAssignments(flattenedAssignments, deactivationReason, deactivationDate);
    }

    void publishDeactivatedFlattenedAssignmentsForDeletion(List<FlattenedAssignment> flattenedAssignments) {
        Set<Long> publishedMembershipIds = new HashSet<>();

        flattenedAssignments.forEach(flattenedAssignment -> {
            UserEntraMembership membership = flattenedAssignment.getUserEntraMembership();
            if (membership == null) {
                log.warn("Skipping removal for flattened assignment {} because it has no user Entra membership", flattenedAssignment.getId());
                return;
            }

            if (!hasOtherActiveFlattenedAssignmentsForMembership(flattenedAssignment, membership)) {
                log.info("User {} and resource {} in deactivated flattened assignment {} has no other active assignment and will be published for deletion",
                        flattenedAssignment.getUserRef(),
                        flattenedAssignment.getResourceRef(),
                        flattenedAssignment.getId()
                );
                if (publishedMembershipIds.add(membership.getId())) {
                    assigmentEntityProducerService.publishRemoval(flattenedAssignment);
                }
            }
            else {
                log.info("User {} and resource {} in deactivated flattened assignment {} has other active assignments and will not be published for deletion",
                        flattenedAssignment.getUserRef(),
                        flattenedAssignment.getResourceRef(),
                        flattenedAssignment.getId()
                );
                log.info("User {} and resource {} still has active assignments, keeping Entra membership active",
                        flattenedAssignment.getUserRef(),
                        flattenedAssignment.getResourceRef()
                );
            }
        });
    }

    public Set<Long> findFlattenedAssignmentIdsByUserAndRoleRef(Long userRef, Long roleRef) {
        return new HashSet<>(flattenedAssignmentRepository.findFlattenedAssignmentIdsByUserAndRoleRef(userRef, roleRef));
    }

    private void terminateFlattenedAssignments(List<FlattenedAssignment> flattenedAssignments, String deactivationReason, Date deactivationDate) {

        flattenedAssignments.forEach(flattenedAssignment -> {
            log.info("Deactivate flattened assignment {}: assignment id {} user {}, resource {}, assigned {}, deactivationReason: {}",
                     flattenedAssignment.getId(),
                     flattenedAssignment.getAssignmentId(),
                        flattenedAssignment.getUserRef(),
                        flattenedAssignment.getResourceRef(),
                        flattenedAssignment.getAssignmentViaRoleRef() == null ? "directly" : "via role " + flattenedAssignment.getAssignmentViaRoleRef(),
                     deactivationReason
            );
            flattenedAssignment.setAssignmentTerminationReason(deactivationReason);
            flattenedAssignment.setAssignmentTerminationDate(deactivationDate);
            flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);
        });
        publishDeactivatedFlattenedAssignmentsForDeletion(flattenedAssignments);
        recalculateAssignedResources(flattenedAssignments);
    }

    private void recalculateAssignedResources(Collection<FlattenedAssignment> flattenedAssignments) {
        flattenedAssignments.stream()
                .map(FlattenedAssignment::getResourceRef)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(this::recalculateAssignedResources);
    }

    private void recalculateAssignedResources(Set<Long> resourceRefs) {
        resourceRefs.stream()
                .filter(Objects::nonNull)
                .sorted()
                .forEach(this::recalculateAssignedResources);
    }

    private void recalculateAssignedResources(Long resourceRef) {
        log.info("Recalculating assigned resources for resource {}", resourceRef);
        licenseEnforcementService.recalculateAssignedResourcesForResource(resourceRef);
    }

    private boolean hasOtherActiveFlattenedAssignmentsForMembership(
            FlattenedAssignment flattenedAssignment,
            UserEntraMembership membership
    ) {
        boolean hasOtherActiveAssignments = flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(membership, flattenedAssignment.getId());

        log.info("{} other active flattened assignments for user Entra membership {}",
                hasOtherActiveAssignments ? "Found" : "No", membership.getId());
        return hasOtherActiveAssignments;
    }

    @Transactional
    public void updateAssignmentsOnUserChange(User user) {
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByUserRefAndAssignmentTerminationDateIsNull(user.getId());
        Map<EntraMembershipKey, UserEntraMembership> membershipsByEntraId = new HashMap<>();
        for (FlattenedAssignment flattenedAssignment : flattenedAssignments) {
            UserEntraMembership membership = flattenedAssignment.getUserEntraMembership();
            if (membership == null) {
                flattenedAssignment.setIdentityProviderUserObjectId(user.getIdentityProviderUserObjectId());
                linkUserEntraMembership(flattenedAssignment, membershipsByEntraId);
            } else {
                migrateUserEntraMembership(membership, user.getIdentityProviderUserObjectId());
            }
        }

        flattenedAssignmentRepository.saveAllAndFlush(flattenedAssignments);
        recalculateAssignedResources(flattenedAssignments);
        log.info("Updated {} flattened assignments for user with id {}", flattenedAssignments.size(), user.getId());
        flattenedAssignments.stream()
                .map(FlattenedAssignment::getUserEntraMembership)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(membership -> assigmentEntityProducerService.publish(membership, true));
    }

    private void linkUserEntraMembership(
            FlattenedAssignment flattenedAssignment,
            Map<EntraMembershipKey, UserEntraMembership> membershipsByEntraId
    ) {
        if (flattenedAssignment.getUserEntraMembership() != null) {
            return;
        }

        if (flattenedAssignment.getIdentityProviderUserObjectId() == null || flattenedAssignment.getIdentityProviderGroupObjectId() == null) {
            log.warn("Skipping user Entra membership linking for flattened assignment {}. Missing user Entra ID ({}) or resource Entra ID ({})",
                    flattenedAssignment.getId(),
                    flattenedAssignment.getIdentityProviderUserObjectId(),
                    flattenedAssignment.getIdentityProviderGroupObjectId());
            return;
        }

        EntraMembershipKey membershipKey = new EntraMembershipKey(
                flattenedAssignment.getIdentityProviderUserObjectId(),
                flattenedAssignment.getIdentityProviderGroupObjectId()
        );
        UserEntraMembership userEntraMembership = membershipsByEntraId.get(membershipKey);
        if (userEntraMembership == null) {
            userEntraMembership = findOrCreateUserEntraMembership(membershipKey);
            membershipsByEntraId.put(membershipKey, userEntraMembership);
        }

        userEntraMembership.addFlattenedAssignment(flattenedAssignment);
        log.debug("Linked flattened assignment {} to user Entra membership {}. userEntraId={}, resourceEntraId={}, entraStatus={}, membershipStatus={}",
                flattenedAssignment.getId(),
                userEntraMembership.getId(),
                userEntraMembership.getUserEntraId(),
                userEntraMembership.getResourceEntraId(),
                userEntraMembership.getEntraStatus(),
                userEntraMembership.getMembershipStatus());
    }

    private UserEntraMembership findOrCreateUserEntraMembership(EntraMembershipKey membershipKey) {
        Optional<UserEntraMembership> existingMembership = userEntraMembershipRepository
                .findByUserEntraIdAndResourceEntraId(membershipKey.userEntraId(), membershipKey.resourceEntraId());

        if (existingMembership.isPresent()) {
            UserEntraMembership membership = existingMembership.get();
            reactivateMembershipIfNeeded(membership);
            return membership;
        }

        log.debug("Creating user Entra membership for user {} and resource {}",
                membershipKey.userEntraId(), membershipKey.resourceEntraId());
        return UserEntraMembership.builder()
                .userEntraId(membershipKey.userEntraId())
                .resourceEntraId(membershipKey.resourceEntraId())
                .entraStatus(EntraMembershipSyncStatus.NOT_SENT)
                .membershipStatus(MembershipStatus.ACTIVE)
                .build();
    }

    private Optional<UserEntraMembership> migrateUserEntraMembership(UserEntraMembership membership, UUID updatedUserEntraId) {
        if (Objects.equals(membership.getUserEntraId(), updatedUserEntraId)) {
            return Optional.empty();
        }

        UserEntraMembership targetMembership = userEntraMembershipRepository
                .findByUserEntraIdAndResourceEntraId(updatedUserEntraId, membership.getResourceEntraId())
                .filter(existingMembership -> !Objects.equals(existingMembership.getId(), membership.getId()))
                .orElse(membership);

        if (targetMembership != membership) {
            List<FlattenedAssignment> linkedAssignments = flattenedAssignmentRepository.findByUserEntraMembership(membership);
            linkedAssignments.forEach(targetMembership::addFlattenedAssignment);
            flattenedAssignmentRepository.saveAllAndFlush(linkedAssignments);
            userEntraMembershipRepository.delete(membership);
        } else {
            membership.setUserEntraId(updatedUserEntraId);
        }

        targetMembership.setMembershipStatus(MembershipStatus.ACTIVE);
        targetMembership.setEntraStatus(EntraMembershipSyncStatus.NOT_SENT);
        targetMembership.setSentToEntraAt(null);
        targetMembership.setDeletionSentToEntraAt(null);
        userEntraMembershipRepository.save(targetMembership);
        return Optional.of(targetMembership);
    }

    private record EntraMembershipKey(UUID userEntraId, UUID resourceEntraId) {
    }

    private void reactivateMembershipIfNeeded(UserEntraMembership userEntraMembership) {
        boolean needsReset =
                userEntraMembership.getMembershipStatus() == MembershipStatus.INACTIVE ||
                        EntraMembershipSyncStatus.inactiveStatuses().contains(userEntraMembership.getEntraStatus());

        if (needsReset) {
            log.debug("Reactivating user Entra membership {}", userEntraMembership.getId());
            userEntraMembership.setEntraStatus(EntraMembershipSyncStatus.NOT_SENT);
            userEntraMembership.setMembershipStatus(MembershipStatus.ACTIVE);
            userEntraMembership.setSentToEntraAt(null);
            userEntraMembership.setDeletionSentToEntraAt(null);
        }
    }

    private int publishUnsentMemberships(List<FlattenedAssignment> savedAssignments) {
        List<UserEntraMembership> membershipsToPublish = savedAssignments.stream()
                .map(FlattenedAssignment::getUserEntraMembership)
                .filter(Objects::nonNull)
                .filter(membership -> membership.getEntraStatus() == EntraMembershipSyncStatus.NOT_SENT)
                .distinct()
                .toList();

        membershipsToPublish.forEach(membership -> assigmentEntityProducerService.publish(membership, false));
        return membershipsToPublish.size();
    }

}
