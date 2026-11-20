package no.fintlabs.device.groupmembership;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceGroupMembershipId implements Serializable {
    private Long deviceId;
    private Long deviceGroupId;
}