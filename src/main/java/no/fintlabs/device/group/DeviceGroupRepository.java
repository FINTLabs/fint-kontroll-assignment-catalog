package no.fintlabs.device.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long>, JpaSpecificationExecutor<DeviceGroup> {

}
