package no.fintlabs.device.group;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;


@Entity
@Table(name = "device_groups")
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
@Data
public class DeviceGroup {
    @Id
    private Long id;

    private Long sourceId;

    private String name;

    private String orgUnitId;

    private String platform;

    private String deviceType;

    @CreationTimestamp
    @EqualsAndHashCode.Exclude
    private Date createdDate;

    @UpdateTimestamp
    @EqualsAndHashCode.Exclude
    private Date modifiedDate;

    private long noOfMembers;
}
