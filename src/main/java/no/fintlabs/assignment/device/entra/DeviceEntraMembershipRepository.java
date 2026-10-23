package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEntraMembershipRepository extends JpaRepository<DeviceEntraMembership, Long> {
    Optional<DeviceEntraMembership> findByDeviceEntraIdAndResourceEntraId(UUID deviceEntraId, UUID resourceEntraId);

    boolean existsByDeviceEntraIdAndResourceEntraIdAndMembershipStatus(UUID deviceEntraId, UUID resourceEntraId, MembershipStatus membershipStatus);

    List<DeviceEntraMembership> findAllByEntraStatus(EntraMembershipSyncStatus entraStatus);

    List<DeviceEntraMembership> findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus entraStatus, UUID deviceEntraId);

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
