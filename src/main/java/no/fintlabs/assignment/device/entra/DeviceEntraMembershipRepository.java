package no.fintlabs.assignment.device.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEntraMembershipRepository extends JpaRepository<DeviceEntraMembership, Long> {
    Optional<DeviceEntraMembership> findByDeviceEntraIdAndResourceEntraId(UUID deviceEntraId, UUID resourceEntraId);

    boolean existsByDeviceEntraIdAndResourceEntraIdAndMembershipStatus(UUID deviceEntraId, UUID resourceEntraId, MembershipStatus membershipStatus);

    List<DeviceEntraMembership> findAllByEntraStatus(EntraMembershipSyncStatus entraStatus);

    List<DeviceEntraMembership> findAllByEntraStatusAndDeviceEntraId(EntraMembershipSyncStatus entraStatus, UUID deviceEntraId);

    long countByResourceEntraIdAndMembershipStatus(UUID resourceEntraId, MembershipStatus membershipStatus);
}
