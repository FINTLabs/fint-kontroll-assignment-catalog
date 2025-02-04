package no.fintlabs.assignment;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.audit.AuditEntity;
import no.fintlabs.resource.Resource;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name = "Assignments")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC, force = true)
//@Where(clause = "disabled = 'false'")
public class Assignment extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String assignmentId;
    @Column(name = "role_ref")
    private Long roleRef;
    private String roleName;
    private String roleType;
    @Column(name = "user_ref")
    private Long userRef;
    private UUID azureAdUserId;
    private String userFirstName;
    private String userLastName;
    private String userUserType;
    @Column(name = "resource_ref")
    private Long resourceRef;
    private String resourceName;
    private UUID azureAdGroupId;
    private String organizationUnitId;
    private String resourceConsumerOrgUnitId;
    private Long assignerRef;
    private Long assignerRemoveRef;
    private Date assignmentRemovedDate;
    private String assignerUserName;
    private UUID assignerAzureAdUserId;
    private Long assignerRoleRef;
    @CreationTimestamp
    private Date assignmentDate;
    private Date validFrom;
    private Date validTo;
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "user_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value = "user-assignment")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "role_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value = "role-assignment")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name = "resource_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value = "resource-assignment")
    private Resource resource;

    public SimpleAssignment toSimpleAssignment() {
        String displayname;

        if (userRef != null) {
            displayname = userFirstName + ' ' + userLastName;
        } else {
            displayname = roleName;
        }
        return SimpleAssignment
                .builder()
                .id(id)
                .resourceRef(resourceRef)
                .azureGroupRef(azureAdGroupId)
                .resourceName(resourceName)
                .userRef(userRef)
                .azureUserRef(azureAdUserId)
                .userDisplayname(displayname)
                .assignerUsername(assignerUserName)
                .assignerRef(assignerRef)
                .userType(userUserType)
                .roleRef(roleRef)
                .organizationUnitId(organizationUnitId)
                .build();
    }

    public String assignmentIdSuffix() {
        return userRef != null ? userRef + "_user" : roleRef + "_role";
    }

    @JsonIgnore
    public boolean isUserAssignment() {
        return userRef != null && azureAdUserId != null;
    }

    @JsonIgnore
    public boolean isGroupAssignment() {
        return roleRef != null && azureAdGroupId != null;
    }
}
