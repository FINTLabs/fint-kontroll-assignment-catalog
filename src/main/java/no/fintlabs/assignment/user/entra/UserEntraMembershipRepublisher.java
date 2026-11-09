package no.fintlabs.assignment.user.entra;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserEntraMembershipRepublisher {

    private static final int BATCH_SIZE = 500;
    private static final Duration FAILED_RETRY_DELAY = Duration.ofHours(24);

    private final UserEntraMembershipRepository membershipRepository;
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final UserEntraMembershipPublisher publisher;
    private final long responseTimeoutHours;

    public UserEntraMembershipRepublisher(
            UserEntraMembershipRepository membershipRepository,
            FlattenedAssignmentRepository flattenedAssignmentRepository,
            UserEntraMembershipPublisher publisher,
            @Value("${fint.kontroll.assignment-catalog.reconciliation.response-timeout:72}") long responseTimeoutHours
    ) {
        this.membershipRepository = membershipRepository;
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.publisher = publisher;
        this.responseTimeoutHours = responseTimeoutHours;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.reconciliation.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.reconciliation.fixed-delay}"
    )
    public void republishStaleMemberships() {
        log.info("Starting user Entra membership republishing job");
        Instant now = Instant.now();
        Date responseCutoff = Date.from(now.minus(Duration.ofHours(responseTimeoutHours)));
        Date failedCutoff = Date.from(now.minus(FAILED_RETRY_DELAY));

        long processed = retryStaleMemberships(responseCutoff, failedCutoff);
        log.info("User Entra membership republishing job finished: {} memberships processed", processed);
    }

    public void resendAddCommandsForActiveAssignments() {
        long lastProcessedId = 0L;
        List<UserEntraMembership> memberships;

        do {
            memberships = membershipRepository.findActiveMembershipsByIdGreaterThanOrderByIdAsc(
                    lastProcessedId, PageRequest.of(0, BATCH_SIZE));
            publisher.republishBatch(memberships);

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

    public long retryStaleMemberships(Date responseCutoff, Date failedCutoff) {
        long lastProcessedId = 0L;
        long processed = 0L;
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
            processed += memberships.size();

            if (!memberships.isEmpty()) {
                lastProcessedId = memberships.getLast().getId();
            }
        } while (memberships.size() == BATCH_SIZE);
        return processed;
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
