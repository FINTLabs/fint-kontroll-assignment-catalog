package no.fintlabs.device.entraInfo;

import no.fintlabs.device.EntraStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEntraInfoRepository extends JpaRepository<DeviceEntraInfo, Long> {
    Optional<DeviceEntraInfo> findByDeviceAzureIdAndResourceAzureId(UUID deviceAzureId, UUID resourceAzureId);

    List<DeviceEntraInfo> findAllByEntraStatus(EntraStatus entraStatus);

    List<DeviceEntraInfo> findAllByEntraStatusAndDeviceAzureId(EntraStatus entraStatus, UUID deviceAzureId);
}
