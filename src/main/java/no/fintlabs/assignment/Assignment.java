package no.fintlabs.assignment;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.Resource;
import no.fintlabs.role.Role;
import no.fintlabs.user.User;

import jakarta.persistence.*;
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
@Table(name="Assignments")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class Assignment {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    private String assignmentId;
    @Column(name="role_ref")
    private Long roleRef;
    private String roleName;
    private String roleType;
    @Column(name="user_ref")
    private Long userRef;
    private UUID azureAdUserId;
    private String userFirstName;
    private String userLastName;
    private String userUserType;
    @Column(name="resource_ref")
    private Long resourceRef;
    private String resourceName;
    private UUID azureAdGroupId;
    private String organizationUnitId;
    private Long assignerRef;
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
            name="user_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value="user-assignment")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name="role_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value="role-assignment")
    private Role role;
    @ManyToOne(fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE})
    @JoinColumn(
            name="resource_ref",
            insertable = false,
            updatable = false)//, nullable = false
    @JsonBackReference(value="resource-assignment")
    private Resource resource;

    public SimpleAssignment toSimpleAssignment() {
        String displayname;
        
        if (userRef!=null) {
            displayname = userFirstName + ' ' + userLastName;
        }
        else {
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
                .userType(userUserType)
                .roleRef(roleRef)
                .organizationUnitId(organizationUnitId)
                .build();
    }
    public DetailedAssignment toDetailedAssignment() {
        return DetailedAssignment
                .builder()
                .id(id)
                .resourceRef(resourceRef)
                .resourceName(resourceName)
                .userRef(userRef)
                .userFirstName(userFirstName)
                .userLastName(userLastName)
                .userType(userUserType)
                .roleRef(roleRef)
                .organizationUnitId(organizationUnitId)
                .AssignerRoleRef(assignerRef)
                .assignmentDate(assignmentDate)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }
}
