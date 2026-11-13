package no.fintlabs.assignment.device.projection;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.assignment.device.entra.DeviceEntraMembership;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
@Table(name = "flattened_device_assignments")
public class FlattenedDeviceAssignment  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NonNull
    private Long assignmentId;
    @Column(nullable = false)
    private Long resourceRef;
    @Column(name = "resource_consumer_org_unit_id")
    private String applicationResourceLocationOrgUnitId;
    private Date assignmentCreationDate;
    private Date terminationDate;

    private String terminationReason;
    private Long deviceRef;
    // Runtime-only mapping values. DeviceEntraMembership owns the persisted Entra pair.
    @Transient
    private UUID identityProviderDeviceObjectId;
    @Transient
    private UUID identityProviderGroupObjectId;
    private Long assignmentViaGroupRef;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private Date modifiedDate;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "device_entra_membership_id", nullable = false)
    private DeviceEntraMembership deviceEntraMembership;

    public UUID getIdentityProviderDeviceObjectId() {
        return deviceEntraMembership == null ? identityProviderDeviceObjectId : deviceEntraMembership.getDeviceEntraId();
    }

    public UUID getIdentityProviderGroupObjectId() {
        return deviceEntraMembership == null ? identityProviderGroupObjectId : deviceEntraMembership.getResourceEntraId();
    }

}
