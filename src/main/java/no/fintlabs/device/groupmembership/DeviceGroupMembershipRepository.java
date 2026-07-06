package no.fintlabs.device.groupmembership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import no.fintlabs.entra.MembershipStatus;

@Repository
public interface DeviceGroupMembershipRepository extends JpaRepository<DeviceGroupMembership, DeviceGroupMembershipId> {

    @Query("select dgm from DeviceGroupMembership dgm where dgm.deviceGroupId = :deviceGroupRef and dgm.membershipStatus = 'ACTIVE'")
    List<DeviceGroupMembership> findAllActiveByDeviceGroupRef(Long deviceGroupRef);

    @Query("""
            SELECT COUNT(DISTINCT device.dataObjectId)
            FROM DeviceGroupMembership membership
            JOIN Device device ON device.id = membership.deviceId
            WHERE membership.deviceGroupId = :deviceGroupRef
            AND UPPER(membership.membershipStatus) = 'ACTIVE'
            AND device.dataObjectId IS NOT NULL
            AND NOT EXISTS (
                SELECT 1
                FROM DeviceEntraMembership deviceEntraMembership
                WHERE deviceEntraMembership.deviceEntraId = device.dataObjectId
                AND deviceEntraMembership.resourceEntraId = :resourceEntraId
                AND deviceEntraMembership.membershipStatus = :membershipStatus
            )
            """)
    long countActiveDeviceGroupMembersWithoutActiveEntraMembership(
            @Param("deviceGroupRef") Long deviceGroupRef,
            @Param("resourceEntraId") UUID resourceEntraId,
            @Param("membershipStatus") MembershipStatus membershipStatus);
}
