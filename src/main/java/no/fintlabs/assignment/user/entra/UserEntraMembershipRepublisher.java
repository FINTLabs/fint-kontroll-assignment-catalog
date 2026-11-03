package no.fintlabs.assignment.user.entra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEntraMembershipRepublisher {

    private static final int BATCH_SIZE = 500;

    private final UserEntraMembershipRepository membershipRepository;
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final UserEntraMembershipPublisher publisher;

    // This sends ADD commands directly. It does not change the stored send status.
    public void resendAddCommandsForActiveAssignments() {
        long lastProcessedId = 0L;
        List<UserEntraMembership> memberships;

        do {
            memberships = membershipRepository.findActiveMembershipsByIdGreaterThanOrderByIdAsc(
                    lastProcessedId, PageRequest.of(0, BATCH_SIZE));
            memberships.forEach(publisher::republish);

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
    }

    public void retryUnconfirmedMembershipsForAssignment(Long assignmentId) {
        long lastProcessedId = 0L;
        List<UserEntraMembership> memberships;

        do {
            memberships = membershipRepository.findByAssignmentIdAndEntraStatusInAndIdGreaterThanOrderByIdAsc(
                    assignmentId, unconfirmedStatuses(), lastProcessedId, PageRequest.of(0, BATCH_SIZE));
            memberships.forEach(membership -> publisher.publish(membership, false));

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
    }

    public void retryUnconfirmedMemberships() {
        long lastProcessedId = 0L;
        List<UserEntraMembership> memberships;

        do {
            memberships = membershipRepository.findByEntraStatusInAndIdGreaterThanOrderByIdAsc(
                    unconfirmedStatuses(), lastProcessedId, PageRequest.of(0, BATCH_SIZE));
            memberships.forEach(membership -> publisher.publish(membership, false));

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
    }

    public void retryStaleMemberships(Date responseCutoff, Date failedCutoff) {
        long lastProcessedId = 0L;
        List<UserEntraMembership> memberships;

        do {
            memberships = membershipRepository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                    List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED),
                    MembershipStatus.ACTIVE,
                    EntraMembershipSyncStatus.SENT,
                    MembershipStatus.INACTIVE,
                    EntraMembershipSyncStatus.DELETION_SENT,
                    EntraMembershipSyncStatus.NEEDS_REPUBLISH,
                    responseCutoff,
                    failedCutoff,
                    lastProcessedId,
                    PageRequest.of(0, BATCH_SIZE));
            memberships.forEach(membership -> publisher.publish(membership, false));

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
    }

    public void forceRepublishMembershipsForFlattenedAssignments(List<Long> selectedIds) {
        Map<Long, UserEntraMembership> membershipsById = new LinkedHashMap<>();

        flattenedAssignmentRepository.findAllById(selectedIds).forEach(assignment -> {
            UserEntraMembership membership = assignment.getUserEntraMembership();
            if (membership == null) {
                log.warn("Skipping selected flattened assignment {} because it has no user Entra membership", assignment.getId());
                return;
            }
            membershipsById.putIfAbsent(membership.getId(), membership);
        });

        membershipsById.values().forEach(membership -> publisher.publish(membership, true));
    }

    private List<EntraMembershipSyncStatus> unconfirmedStatuses() {
        return List.of(
                EntraMembershipSyncStatus.NOT_SENT,
                EntraMembershipSyncStatus.SENT,
                EntraMembershipSyncStatus.TO_BE_DELETED,
                EntraMembershipSyncStatus.DELETION_SENT,
                EntraMembershipSyncStatus.NEEDS_REPUBLISH
        );
    }
}
