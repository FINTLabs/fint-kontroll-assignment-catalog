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
                    AND membership.entraStatus IN :addStatuses
                    AND (membership.sentToEntraAt IS NULL OR membership.sentToEntraAt < :cutoff))
                OR
                (membership.membershipStatus = :inactiveStatus
                    AND membership.entraStatus IN :removeStatuses
                    AND (membership.deletionSentToEntraAt IS NULL OR membership.deletionSentToEntraAt < :cutoff))
            )
            ORDER BY membership.id ASC
            """)
    List<DeviceEntraMembership> findStaleMembershipsByIdGreaterThanOrderByIdAsc(
            @Param("unpublishedStatuses") List<EntraMembershipSyncStatus> unpublishedStatuses,
            @Param("activeStatus") MembershipStatus activeStatus,
            @Param("addStatuses") List<EntraMembershipSyncStatus> addStatuses,
            @Param("inactiveStatus") MembershipStatus inactiveStatus,
            @Param("removeStatuses") List<EntraMembershipSyncStatus> removeStatuses,
            @Param("cutoff") Date cutoff,
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
