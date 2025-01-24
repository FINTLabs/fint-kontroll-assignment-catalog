package no.fintlabs.assignment.flattened;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String resourceConsumerOrgUnitId;
    private UUID identityProviderGroupObjectId;
    @Builder.Default
    private boolean identityProviderGroupMembershipConfirmed = false;
    @Builder.Default
    private boolean identityProviderGroupMembershipDeletionConfirmed = false;
    private Long assignmentViaRoleRef;
    private Date assignmentCreationDate;
    private Date assignmentTerminationDate;
    private String assignmentTerminationReason;
}
