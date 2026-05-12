package no.fintlabs.device.entra;

import jakarta.persistence.*;
import lombok.*;
import no.fintlabs.device.EntraStatus;
import no.fintlabs.device.MembershipStatus;
import no.fintlabs.device.assignment.FlattenedDeviceAssignment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "device_entra_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_resource",
                        columnNames = {"device_entra_id", "resource_entra_id"}
                )
        })
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
    @Column(name = "entra_status", nullable = false)
    private EntraStatus entraStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    private MembershipStatus membershipStatus;
    @Column(name = "sent_to_entra_at")
    private Date sentToEntraAt;

    @Column(name = "deletion_sent_to_entra_at")
    private Date deletionSentToEntraAt;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
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
