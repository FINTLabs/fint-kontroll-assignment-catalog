package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.entra.UserEntraMembership;
import no.fintlabs.assignment.entra.UserEntraMembershipRepository;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
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
public class FlattenedAssignmentService {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    private final FlattenedAssignmentMapper flattenedAssignmentMapper;
    private final UserEntraMembershipRepository userEntraMembershipRepository;
    private final LicenseEnforcementService licenseEnforcementService;

    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public FlattenedAssignmentService(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                      FlattenedAssignmentMapper flattenedAssignmentMapper,
                                      FlattenedAssignmentMembershipService flattenedAssignmentMembershipService,
                                      UserEntraMembershipRepository userEntraMembershipRepository,
                                      LicenseEnforcementService licenseEnforcementService,
                                      AssigmentEntityProducerService assigmentEntityProducerService) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.flattenedAssignmentMembershipService = flattenedAssignmentMembershipService;
        this.flattenedAssignmentMapper = flattenedAssignmentMapper;
        this.userEntraMembershipRepository = userEntraMembershipRepository;
        this.licenseEnforcementService = licenseEnforcementService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    @Async
    @Transactional
    public void createFlattenedAssignments(Assignment assignment) {
        createFlattenedAssignmentsSync(assignment);
    }

    @Transactional
    public void createFlattenedAssignmentsSync(Assignment assignment) {
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
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, false);
        }
    }

    @Async
    @Transactional
    public void syncFlattenedAssignments(Assignment assignment, boolean isSync) {
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
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingActiveAssignments) //, isSync
                    .ifPresent(flattenedAssignments::add);
        } else if (assignment.isGroupAssignment()) {
            flattenedAssignments.addAll(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingActiveAssignments)); //, isSync
        } else {
            log.error("Assignment with id {} is not a user or group assignment, not updating any flattened assignment", assignment.getId());
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, isSync);
        }
    }

    @Transactional
    public void createOrUpdateFlattenedAssignmentsForMembership(Assignment assignment, Membership membership) {
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

            saveAndPublishNewFlattenedAssignment(mappedFlattenedAssignment, false);
            return;
        }
        log.info("Found {} existing flattened assignments for role {}, user {} and assignment {}. Updating if needed",
                existingFlattenedAssignments.size(),
                roleRef,
                userRef,
                assignment.getId()
        );

        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        if (!membership.isActive()) {
            Date now = new Date();
            for (FlattenedAssignment flattenedAssignment : existingFlattenedAssignments) {
                if (flattenedAssignment.getAssignmentTerminationDate() == null) {
                    log.info("Terminating flattened assignment {} due to inactive membership (role {}, user {}, assignment {})",
                            flattenedAssignment.getId(), roleRef, userRef, assignment.getId());
                    flattenedAssignment.setAssignmentTerminationDate(now);
                    flattenedAssignments.add(flattenedAssignment);
                }
            }
        } else {
            for (FlattenedAssignment flattenedAssignment : existingFlattenedAssignments) {
                UUID newIdp = membership.getIdentityProviderUserObjectId();
                if (newIdp != null && !newIdp.equals(flattenedAssignment.getIdentityProviderUserObjectId())) {
                    log.info("Updating identityProviderUserObjectId on flattened assignment {} (role {}, user {}, assignment {})",
                            flattenedAssignment.getId(), roleRef, userRef, assignment.getId());
                    flattenedAssignment.setIdentityProviderUserObjectId(newIdp);
                    flattenedAssignments.add(flattenedAssignment);
                }
            }
        }

        if (!flattenedAssignments.isEmpty()) {
            saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, false);
        }
    }

    public void saveFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate) {
        log.info("saveFlattened - Saving {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            List<FlattenedAssignment> savedFlattened = flattenedAssignmentRepository.saveAll(batch);
            savedFlattened.forEach(flattenedAssignment ->
                log.info("saveFlattened - Saved flattened assignment with id: {}, assignmentId: {}", flattenedAssignment.getId(), flattenedAssignment.getAssignmentId())
            );
        }

        flattenedAssignmentRepository.flush();

        log.info("saveFlattened - Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    public void saveAndPublishNewFlattenedAssignment(FlattenedAssignment newflattenedAssignment,boolean isSync ) {
        log.info("saveAndPublishNewFlattenedAssignment - Saving new flattened assignment for role {}, user {} and assignment {}",
                newflattenedAssignment.getAssignmentViaRoleRef(),
                newflattenedAssignment.getUserRef(),
                newflattenedAssignment.getAssignmentId());
        addUserEntraMembership(newflattenedAssignment);
        FlattenedAssignment savedFlattened = flattenedAssignmentRepository.saveAndFlush(newflattenedAssignment);
        log.info("saveAndPublishNewFlattenedAssignment - Saved new flattened assignment with id: {}, assignmentId: {}",
                savedFlattened.getId(),
                savedFlattened.getAssignmentId());

        if (!isSync) {
            log.info("saveAndPublishNewFlattenedAssignment - Publishing new flattened assignment to Entra ID");
            publishNewMembership(savedFlattened);
        }
        recalculateAssignedResources(List.of(savedFlattened));
    }

    public void saveAndPublishFlattenedAssignmentsBatch(List<FlattenedAssignment> flattenedAssignmentsForUpdate, boolean isSync) {
        log.info("saveAndPublish - {} flattened assignments", flattenedAssignmentsForUpdate.size());
        int batchSize = 800;
        Set<Long> affectedResourceRefs = new HashSet<>();

        for (int i = 0; i < flattenedAssignmentsForUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, flattenedAssignmentsForUpdate.size());
            List<FlattenedAssignment> batch = flattenedAssignmentsForUpdate.subList(i, end);
            batch.forEach(this::addUserEntraMembership);
            List<FlattenedAssignment> savedFlattened = flattenedAssignmentRepository.saveAll(batch);
            savedFlattened.stream()
                    .map(FlattenedAssignment::getResourceRef)
                    .filter(Objects::nonNull)
                    .forEach(affectedResourceRefs::add);
            savedFlattened.forEach(flattenedAssignment ->
                log.info("saveAndPublish - Flattened assignment with id: {}, assignmentId: {}", flattenedAssignment.getId(), flattenedAssignment.getAssignmentId())
            );

            if (!isSync) {
                log.info("saveAndPublish - Publishing {} new flattened assignments to Entra ID", batch.size());
                savedFlattened.stream()
                        .map(FlattenedAssignment::getUserEntraMembership)
                        .filter(Objects::nonNull)
                        .filter(userEntraMembership -> userEntraMembership.getEntraStatus().equals(EntraStatus.NOT_SENT))
                        .distinct()
                        .forEach(userEntraMembership -> assigmentEntityProducerService.publish(userEntraMembership, false));
            }
        }

        flattenedAssignmentRepository.flush();
        recalculateAssignedResources(affectedResourceRefs);

        log.info("saveAndPublish - Saved {} flattened assignments", flattenedAssignmentsForUpdate.size());
    }

    @Transactional
    public void deleteFlattenedAssignments(Assignment assignment, String deactivationReason) {
        log.info("Deactivate flattened assignments for assignment with id {}", assignment.getId());

        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());
        saveDeactivatedFlattenedAssignments(flattenedAssignments, deactivationReason, assignment.getAssignmentRemovedDate());
        publishDeactivatedFlattenedAssignmentsForDeletion(flattenedAssignments);
        recalculateAssignedResources(flattenedAssignments);
    }

    @Transactional
    public void deactivateFlattenedAssignments(Set<Long> flattenedAssignmentIds) {
        log.info("Deactivate flattened assignments:  {}", flattenedAssignmentIds);

        String deactivationReason = "Role membership deactivated";
        Date deactivationDate = new Date();
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentIds
                .stream()
                .map(flattenedAssignmentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        saveDeactivatedFlattenedAssignments(flattenedAssignments, deactivationReason, deactivationDate);
        publishDeactivatedFlattenedAssignmentsForDeletion(flattenedAssignments);
        recalculateAssignedResources(flattenedAssignments);
    }

    public void publishDeactivatedFlattenedAssignmentsForDeletion(List<FlattenedAssignment> flattenedAssignments) {
        flattenedAssignments.forEach(flattenedAssignment -> {
            if (notExistOtherActiveFlattenedAssignmentsWithSameUserRefAndResourceRef(flattenedAssignment)) {
                log.info("User {} and resource {} in deactivated flattened assignment {} has no other active assignment and will be published for deletion",
                        flattenedAssignment.getUserRef(),
                        flattenedAssignment.getResourceRef(),
                        flattenedAssignment.getId()
                );
                assigmentEntityProducerService.publishDeletion(flattenedAssignment);
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

    public List<FlattenedAssignment> getAllFlattenedAssignments() {
        return flattenedAssignmentRepository.findAll();
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmed() {
        return flattenedAssignmentRepository.findByUserEntraMembershipEntraStatusInAndAssignmentTerminationDateIsNull(unconfirmedActiveStatuses());
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsIdentityProviderGroupMembershipNotConfirmedByAssignmentId(Long assignmentId) {
        return flattenedAssignmentRepository.findByUserEntraMembershipEntraStatusInAndAssignmentTerminationDateIsNullAndAssignmentId(unconfirmedActiveStatuses(), assignmentId);
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsDeletedNotConfirmed() {
        return flattenedAssignmentRepository.findByUserEntraMembershipEntraStatusInAndAssignmentTerminationDateIsNotNull(unconfirmedInactiveStatuses());
    }

    public List<FlattenedAssignment> getFlattenedAssignmentsDeletedNotConfirmedByAssignmentId(Long assignmentId) {
        return flattenedAssignmentRepository.findByUserEntraMembershipEntraStatusInAndAssignmentTerminationDateIsNotNullAndAssignmentId(unconfirmedInactiveStatuses(), assignmentId);
    }

    public Optional<FlattenedAssignment> getFlattenedAssignmentByUserAndResourceNotTerminated(Long userRef, Long resourceRef) {
        return flattenedAssignmentRepository.findByUserRefAndResourceRefAndAssignmentTerminationDateIsNull(userRef, resourceRef);
    }

    public Set<Long> findFlattenedAssignmentIdsByUserAndRoleRef(Long userRef, Long roleRef) {
        return new HashSet<>(flattenedAssignmentRepository.findFlattenedAssignmentIdsByUserAndRoleRef(userRef, roleRef));
    }

    public Set<Long> getIdsMissingIdentityProviderUserObjectId() {
        return new HashSet<>(flattenedAssignmentRepository.findIdsWhereIdentityProviderUserObjectIdIsNull());
    }

    private void saveDeactivatedFlattenedAssignments(List<FlattenedAssignment> flattenedAssignments, String deactivationReason, Date deactivationDate) {

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
    }

    private void recalculateAssignedResources(Collection<FlattenedAssignment> flattenedAssignments) {
        flattenedAssignments.stream()
                .map(FlattenedAssignment::getResourceRef)
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

    private boolean notExistOtherActiveFlattenedAssignmentsWithSameUserRefAndResourceRef(FlattenedAssignment flattenedAssignment) {

        log.info("Checking if other active flattened assignment exists for user {} and resource {}",
                flattenedAssignment.getUserRef(),
                flattenedAssignment.getResourceRef()
        );
        List<FlattenedAssignment> otherActiveAssignments = flattenedAssignmentRepository.findByAssignmentViaRoleRefNotAndUserRefAndResourceRefAndAssignmentTerminationDateIsNull(
                flattenedAssignment.getAssignmentViaRoleRef(),
                flattenedAssignment.getUserRef(),
                flattenedAssignment.getResourceRef()
        );

        if (otherActiveAssignments.isEmpty()) {
            log.info("No other active flattened assignment found for user {} and resource {}, proceeding with deletion",
                    flattenedAssignment.getUserRef(),
                    flattenedAssignment.getResourceRef()
            );
            return true;
        }
        otherActiveAssignments.forEach(otherAssignment ->
            log.info("Found active flattened assignment {} for user {} and resource {} assigned {}",
                    otherAssignment.getId(),
                    otherAssignment.getUserRef(),
                    otherAssignment.getResourceRef(),
                    otherAssignment.getAssignmentViaRoleRef() == null ? "directly" : "via role " + otherAssignment.getAssignmentViaRoleRef()
            )
        );
        return false;
    }

    public void publishAllActive(Assignment assignment) {
        log.info("Publishing all flattened assignments for assignment with id {}", assignment.getId());
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(assignment.getId());
        flattenedAssignments.stream().filter(f -> f.getAssignmentTerminationDate() == null).forEach(flattenedAssignment -> assigmentEntityProducerService.publish(flattenedAssignment, true));
    }

    public void updateAssignmentsOnUserChange(User user) {
        List<FlattenedAssignment> flattenedAssignments = flattenedAssignmentRepository.findByUserRefAndAssignmentTerminationDateIsNull(user.getId());
        flattenedAssignments.forEach(flattenedAssignment -> flattenedAssignment.setIdentityProviderUserObjectId(user.getIdentityProviderUserObjectId()));
        flattenedAssignments.forEach(this::addUserEntraMembership);
        flattenedAssignmentRepository.saveAll(flattenedAssignments);
        log.info("Updated {} flattened assignments for user with id {}", flattenedAssignments.size(), user.getId());
        flattenedAssignments.forEach(flattenedAssignment -> assigmentEntityProducerService.publish(flattenedAssignment, true));
    }

    public void republishUnconfirmedFlattenedAssignments() {
        flattenedAssignmentRepository.findByUserEntraMembershipEntraStatusInAndAssignmentTerminationDateIsNull(unconfirmedActiveStatuses())
                .parallelStream()
                .forEach(assigmentEntityProducerService::publish);
    }

    public void republishSelectedFlattenedAssignments(List<Long> selectedIds) {
        flattenedAssignmentRepository.findByAssignmentTerminationDateIsNullAndIdIn(selectedIds)
                .parallelStream()
                .forEach(flattenedAssignment -> assigmentEntityProducerService.publish(flattenedAssignment, true));
    }

    private void addUserEntraMembership(FlattenedAssignment flattenedAssignment) {
        if (flattenedAssignment.getIdentityProviderUserObjectId() == null || flattenedAssignment.getIdentityProviderGroupObjectId() == null) {
            log.warn("Skipping user Entra membership linking for flattened assignment {}. Missing user Entra ID ({}) or resource Entra ID ({})",
                    flattenedAssignment.getId(),
                    flattenedAssignment.getIdentityProviderUserObjectId(),
                    flattenedAssignment.getIdentityProviderGroupObjectId());
            return;
        }

        UserEntraMembership userEntraMembership = userEntraMembershipRepository
                .findByUserEntraIdAndResourceEntraId(flattenedAssignment.getIdentityProviderUserObjectId(), flattenedAssignment.getIdentityProviderGroupObjectId())
                .map(this::getEntraMembershipForNewFlattenedAssignment)
                .orElseGet(() -> UserEntraMembership.builder()
                        .userEntraId(flattenedAssignment.getIdentityProviderUserObjectId())
                        .resourceEntraId(flattenedAssignment.getIdentityProviderGroupObjectId())
                        .entraStatus(EntraStatus.NOT_SENT)
                        .membershipStatus(MembershipStatus.ACTIVE)
                        .build()
                );

        userEntraMembership.addFlattenedAssignment(flattenedAssignment);
    }

    private UserEntraMembership getEntraMembershipForNewFlattenedAssignment(UserEntraMembership userEntraMembership) {
        boolean needsReset =
                userEntraMembership.getMembershipStatus() == MembershipStatus.INACTIVE ||
                        EntraStatus.inactiveStatuses().contains(userEntraMembership.getEntraStatus());

        if (needsReset) {
            userEntraMembership.setEntraStatus(EntraStatus.NOT_SENT);
            userEntraMembership.setMembershipStatus(MembershipStatus.ACTIVE);
            userEntraMembership.setSentToEntraAt(null);
            userEntraMembership.setDeletionSentToEntraAt(null);
        }

        return userEntraMembership;
    }

    private void publishNewMembership(FlattenedAssignment flattenedAssignment) {
        UserEntraMembership userEntraMembership = flattenedAssignment.getUserEntraMembership();
        if (userEntraMembership != null && userEntraMembership.getEntraStatus().equals(EntraStatus.NOT_SENT)) {
            assigmentEntityProducerService.publish(userEntraMembership, false);
        }
    }

    private List<EntraStatus> unconfirmedActiveStatuses() {
        return List.of(EntraStatus.NOT_SENT, EntraStatus.SENT, EntraStatus.ERROR, EntraStatus.NEEDS_REPUBLISH);
    }

    private List<EntraStatus> unconfirmedInactiveStatuses() {
        return List.of(EntraStatus.TO_BE_DELETED, EntraStatus.DELETION_SENT, EntraStatus.ERROR, EntraStatus.NEEDS_REPUBLISH);
    }
}
