package no.fintlabs.assignment.entra;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.entra.EntraStatus;
import no.fintlabs.entra.MembershipStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_entra_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_resource",
                        columnNames = {"user_entra_id", "resource_entra_id"}
                )
        })
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserEntraMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_entra_id", nullable = false)
    private UUID userEntraId;

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

    @OneToMany(mappedBy = "userEntraMembership", cascade = CascadeType.PERSIST)
    @SQLRestriction("assignment_termination_date IS NULL")
    @Builder.Default
    private List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

    public void addFlattenedAssignment(FlattenedAssignment assignment) {
        flattenedAssignments.add(assignment);
        assignment.setUserEntraMembership(this);
    }
}
