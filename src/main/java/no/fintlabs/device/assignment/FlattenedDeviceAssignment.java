package no.fintlabs.device.assignment;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.device.azureInfo.DeviceAzureInfo;
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
    private Long resourceRef;
    @Column(name = "resource_consumer_org_unit_id")
    private String applicationResourceLocationOrgUnitId;
    private UUID identityProviderGroupObjectId;
    private Date assignmentCreationDate;
    private Date terminationDate;

    private String terminationReason;
    private Long deviceRef;
    private UUID identityProviderDeviceObjectId;
    private Long assignmentViaGroupRef;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private Date modifiedDate;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "azure_info_id", updatable = false)
    private DeviceAzureInfo azureInfo;

}
