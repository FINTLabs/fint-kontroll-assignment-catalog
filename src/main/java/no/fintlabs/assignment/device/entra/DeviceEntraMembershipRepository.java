package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Date;
import java.util.UUID;

public interface DeviceEntraMembershipRepository extends JpaRepository<DeviceEntraMembership, Long> {
    Optional<DeviceEntraMembership> findByDeviceEntraIdAndResourceEntraId(UUID deviceEntraId, UUID resourceEntraId);
    
    List<DeviceEntraMembership> findAllByEntraStatus(EntraMembershipSyncStatus entraStatus);

    List<DeviceEntraMembership> findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus entraStatus, UUID deviceEntraId);

    @Query("""
            SELECT membership
            FROM DeviceEntraMembership membership
            WHERE membership.id > :lastProcessedId
            AND (
                membership.entraStatus IN :unpublishedStatuses
                OR
                (membership.membershipStatus = :activeStatus
                    AND ((membership.entraStatus = :sentStatus
                        AND (membership.sentToEntraAt IS NULL OR membership.sentToEntraAt < :responseCutoff))
                    OR (membership.entraStatus = :needsRepublishStatus
                        AND (membership.sentToEntraAt IS NULL OR membership.sentToEntraAt < :failedCutoff))))
                OR
                (membership.membershipStatus = :inactiveStatus
                    AND ((membership.entraStatus = :deletionSentStatus
                        AND (membership.deletionSentToEntraAt IS NULL OR membership.deletionSentToEntraAt < :responseCutoff))
                    OR (membership.entraStatus = :needsRepublishStatus
                        AND (membership.deletionSentToEntraAt IS NULL OR membership.deletionSentToEntraAt < :failedCutoff))))
            )
            ORDER BY membership.id ASC
            """)
    List<DeviceEntraMembership> findStaleMembershipsByIdGreaterThanOrderByIdAsc(
            @Param("unpublishedStatuses") List<EntraMembershipSyncStatus> unpublishedStatuses,
            @Param("activeStatus") MembershipStatus activeStatus,
            @Param("sentStatus") EntraMembershipSyncStatus sentStatus,
            @Param("inactiveStatus") MembershipStatus inactiveStatus,
            @Param("deletionSentStatus") EntraMembershipSyncStatus deletionSentStatus,
            @Param("needsRepublishStatus") EntraMembershipSyncStatus needsRepublishStatus,
            @Param("responseCutoff") Date responseCutoff,
            @Param("failedCutoff") Date failedCutoff,
            @Param("lastProcessedId") Long lastProcessedId,
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT membership.id)
            FROM DeviceEntraMembership membership
            JOIN membership.flattenedDeviceAssignments assignment
            WHERE membership.resourceEntraId = :resourceEntraId
            AND membership.membershipStatus = :membershipStatus
            """)
    long countByResourceEntraIdAndMembershipStatus(
            @Param("resourceEntraId") UUID resourceEntraId,
            @Param("membershipStatus") MembershipStatus membershipStatus);
}
