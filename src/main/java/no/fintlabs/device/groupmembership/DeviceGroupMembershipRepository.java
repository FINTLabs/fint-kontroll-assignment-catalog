package no.fintlabs.device.groupmembership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceGroupMembershipRepository extends JpaRepository<DeviceGroupMembership, DeviceGroupMembershipId> {

    @Query("select dgm from DeviceGroupMembership dgm where dgm.deviceGroupId = :deviceGroupRef and dgm.membershipStatus = 'ACTIVE'")
    List<DeviceGroupMembership> findAllActiveByDeviceGroupRef(Long deviceGroupRef);
}
