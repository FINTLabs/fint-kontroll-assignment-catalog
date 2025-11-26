package no.fintlabs.device.azureInfo;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.device.AzureStatus;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "device_azure_info")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceAzureInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "device_azure_id", nullable = false)
    private UUID deviceAzureId;
    @Column(name = "resource_azure_id", nullable = false)
    private UUID resourceAzureId;
    @Enumerated(EnumType.STRING)
    @Column(name = "azure_status", nullable = false)
    private AzureStatus azureStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "kontroll_status", nullable = false)
    private KontrollStatus kontrollStatus;
    private Date sentToAzureAt;
    private Date deletionSentToAzureAt;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @OneToMany(mappedBy = "azureInfo", cascade = CascadeType.PERSIST)
    @SQLRestriction("termination_date IS NULL")
    @Builder.Default
    private List<FlattenedDeviceAssignment> flattenedDeviceAssignments = new ArrayList<>();

    public void addFlattenedAssignment(FlattenedDeviceAssignment assignment) {
        flattenedDeviceAssignments.add(assignment);
        assignment.setAzureInfo(this);
    }



}
