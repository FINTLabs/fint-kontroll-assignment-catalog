package no.fintlabs.device.entra;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.KontrollStatus;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "device_entra_membership")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceEntraMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "device_entra_id", nullable = false)
    private UUID deviceEntraId;
    @Column(name = "resource_entra_id", nullable = false)
    private UUID resourceEntraId;
    @Enumerated(EnumType.STRING)
    @Column(name = "azure_status", nullable = false)
    private EntraStatus entraStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "kontroll_status", nullable = false)
    private KontrollStatus kontrollStatus;
    private Date sentToAzureAt;
    private Date deletionSentToAzureAt;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @OneToMany(mappedBy = "deviceEntraMembership", cascade = CascadeType.PERSIST)
    @SQLRestriction("termination_date IS NULL")
    @Builder.Default
    private List<FlattenedDeviceAssignment> flattenedDeviceAssignments = new ArrayList<>();

    public void addFlattenedAssignment(FlattenedDeviceAssignment assignment) {
        flattenedDeviceAssignments.add(assignment);
        assignment.setDeviceEntraMembership(this);
    }



}
