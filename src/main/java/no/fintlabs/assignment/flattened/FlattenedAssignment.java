package no.fintlabs.assignment.flattened;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import no.fintlabs.assignment.Assignment;
import no.fintlabs.resource.Resource;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;

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
    @Builder.Default
    private boolean identityProviderGroupMembershipConfirmed = false;
    @Builder.Default
    private boolean identityProviderGroupMembershipDeletionConfirmed = false;
    private Long assignmentViaRoleRef;
    private Date assignmentCreationDate;
    private Date assignmentTerminationDate;
    private String assignmentTerminationReason;

    // Used to map the results of the specification query join
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "userRef",
            insertable = false,
            updatable = false)
    @JsonBackReference(value = "user-flattenedassignment")
    private User user;

    // Used to map the results of the specification query join
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "assignmentViaRoleRef",
            insertable = false,
            updatable = false)
    @JsonBackReference(value = "role-flattenedassignment")
    private Role role;

    // Used to map the results of the specification query join
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "assignmentId",
            insertable = false,
            updatable = false)
    @JsonBackReference(value = "assignment-flattenedassignment")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "resourceRef",
            insertable = false,
            updatable = false)
    @JsonBackReference(value = "resource-flattenedassignment")
    private Resource resource;
}
