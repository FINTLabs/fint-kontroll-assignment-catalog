package no.fintlabs.device.entra;

import no.fintlabs.device.EntraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEntraMembershipRepository extends JpaRepository<DeviceEntraMembership, Long> {
    Optional<DeviceEntraMembership> findByDeviceEntraIdAndResourceEntraId(UUID deviceEntraId, UUID resourceEntraId);

    List<DeviceEntraMembership> findAllByEntraStatus(EntraStatus entraStatus);

    List<DeviceEntraMembership> findAllByEntraStatusAndDeviceEntraId(EntraStatus entraStatus, UUID deviceEntraId);
}
