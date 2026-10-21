package no.fintlabs.device;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.UUID;


@Entity
@Table(name = "devices")
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@AllArgsConstructor
@Data
public class Device {
    @Id
    private Long id;

    private String sourceId;

    private String serialNumber;

    private UUID dataObjectId;

    private String name;

    private String platform;

    private String deviceType;

    private String status;

    private Date statusChanged;

    @CreationTimestamp
    @EqualsAndHashCode.Exclude
    private Date createdDate;

    @UpdateTimestamp
    @EqualsAndHashCode.Exclude
    private Date modifiedDate;

    private Boolean isPrivateProperty;

    private Boolean isShared;

}
