package no.fintlabs.device.groupmembership;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.device.Device;

import java.util.Date;

@Entity
@Table(name = "device_group_memberships")
@IdClass(DeviceGroupMembershipId.class)
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
@Data
public class DeviceGroupMembership {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Id
    @Column(name = "device_group_id")
    private Long deviceGroupId;

    private String membershipStatus;

    private Date membershipStatusChanged;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Device device;

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(membershipStatus);
    }
}

