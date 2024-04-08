package no.fintlabs.assignment.flattened;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name="FlattenedAssignments")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class FlattenedAssignment {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    @NonNull
    private Long assignmentId;
    private Long assignerRef;
    private Long userRef;
    private UUID identityProviderUserObjectId;
    private Long resourceRef;
    private UUID identityProviderGroupObjectId;
    private boolean identityProviderGroupMembershipConfirmed = false;
    private boolean identityProviderGroupMembershipDeletionConfirmed = false;
    private Long assignmentViaRoleRef;
    private Date assignmentCreationDate;
    private Date assignmentTerminationDate;
    private String assignmentTerminationReason;
}
