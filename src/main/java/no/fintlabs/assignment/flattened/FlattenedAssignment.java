package no.fintlabs.assignment.flattened;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.audit.AuditEntity;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name = "FlattenedAssignments")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
public class FlattenedAssignment extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NonNull
    private Long assignmentId;
    private Long assignerRef;
    private Long userRef;
    private UUID identityProviderUserObjectId;
    private Long resourceRef;
    //private String licenseEnforcement;
    @Column(name = "resource_consumer_org_unit_id")
    private String applicationResourceLocationOrgUnitId;
    private UUID identityProviderGroupObjectId;
    @Builder.Default
    private boolean identityProviderGroupMembershipConfirmed = false;
    @Builder.Default
    private boolean identityProviderGroupMembershipDeletionConfirmed = false;
    private Long assignmentViaRoleRef;
    private Date assignmentCreationDate;
    private Date assignmentTerminationDate;
    private String assignmentTerminationReason;
    private Instant modifiedDate;

    @PreUpdate
    public void onUpdate() {
        modifiedDate = ZonedDateTime.now(ZoneId.of("Europe/Paris")).toInstant();
    }
}
