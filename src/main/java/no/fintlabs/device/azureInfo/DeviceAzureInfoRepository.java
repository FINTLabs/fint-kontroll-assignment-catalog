package no.fintlabs.device.azureInfo;

import no.fintlabs.device.AzureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceAzureInfoRepository extends JpaRepository<DeviceAzureInfo, Long> {
    Optional<DeviceAzureInfo> findByDeviceAzureIdAndResourceAzureId(UUID deviceAzureId, UUID resourceAzureId);

    List<DeviceAzureInfo> findAllByAzureStatus(AzureStatus azureStatus);

    List<DeviceAzureInfo> findAllByAzureStatusAndDeviceAzureId(AzureStatus azureStatus, UUID deviceAzureId);
}
