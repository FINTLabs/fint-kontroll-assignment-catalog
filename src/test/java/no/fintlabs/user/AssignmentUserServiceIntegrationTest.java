package no.fintlabs.user;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.assignment.user.projection.FlattenedAssignment;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentRepository;
import no.fintlabs.authorization.AuthorizationUtil;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Testcontainers
@Import(AssignmentUserService.class)
class AssignmentUserServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentUserService assignmentUserService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthorizationUtil authorizationUtil;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Autowired
    private UserEntraMembershipRepository userEntraMembershipRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private final String varfk = "varfk";
    private final String kompavd = "kompavd";
    private final String allorgunits = "ALLORGUNITS";

    private final List<String> kompavdList = List.of(kompavd);
    private final List<String> allorgunitsList = List.of(allorgunits);

    private final String zip = "zip";
    private final String adobek12 = "adobek12";
    private final String student = "Student";
    private final String allTypes = "ALLTYPES";

    private final UUID userUUID1 = UUID.fromString("87654321-abcd-1234-abcd-1234567890ab");
    private final UUID userUUID2 = UUID.fromString("12345678-abcd-1234-abcd-1234567890ab");

    User studentUser = User.builder()
        .id(123L)
        .firstName("Test")
        .lastName("Testesen")
        .userName("test")
        .identityProviderUserObjectId(userUUID1)
        .organisationUnitId(kompavd)
        .userType(student)
        .status("ACTIVE")
        .build();

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_user_direct() {
        Resource savedResource = createResource(1L, "1", "ALLTYPES", "Test resource");
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no",  userUUID1, "555", "ALLTYPES");
        Assignment savedAssignment = createAssignment(null, savedUser.getId(), savedUser.getIdentityProviderUserObjectId(), savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = saveFlattenedAssignment(flattenedAssignment);

        testEntityManager.flush();
        testEntityManager.clear();

        Page<ResourceAssignmentUser> resourceAssignmentUsers =
                assignmentUserService.findResourceAssignmentUsersForResourceId(1L, "ALLTYPES", List.of("555"), List.of("555"), null, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        ResourceAssignmentUser resourceAssignmentUser = resourceAssignmentUsers.getContent().get(0);
        assertThat(resourceAssignmentUser.getAssignmentRef()).isEqualTo(savedFlattenedAssignment.getAssignmentId());
        assertThat(resourceAssignmentUser.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(resourceAssignmentUser.getAssignmentViaRoleRef()).isEqualTo(savedAssignment.getRoleRef());
        assertThat(resourceAssignmentUser.isDirectAssignment()).isTrue();

        assertThat(resourceAssignmentUser.getAssigneeUsername()).isEqualTo(savedUser.getUserName());
        assertThat(resourceAssignmentUser.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(resourceAssignmentUser.getAssigneeUserType()).isEqualTo(savedUser.getUserType());
        assertThat(resourceAssignmentUser.getAssigneeOrganisationUnitId()).isEqualTo(savedUser.getOrganisationUnitId());
        assertThat(resourceAssignmentUser.getAssigneeOrganisationUnitName()).isEqualTo(savedUser.getOrganisationUnitName());
        assertThat(resourceAssignmentUser.getAssigneeFirstName()).isEqualTo(savedUser.getFirstName());
        assertThat(resourceAssignmentUser.getAssigneeLastName()).isEqualTo(savedUser.getLastName());

        assertThat(resourceAssignmentUser.getAssignmentViaRoleName()).isNull();
        assertThat(resourceAssignmentUser.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(resourceAssignmentUser.getAssigneeFirstName()).isEqualTo("Test");
        assertThat(resourceAssignmentUser.getAssigneeLastName()).isEqualTo("Testesen");
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_user_inDirect() {
        Resource savedResource = createResource(1L, "1", "ALLTYPES", "Test resource");
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no",  userUUID1, "555", "ALLTYPES");
        Role savedRole = createRole();
        Assignment savedAssignment = createAssignment(savedRole.getId(), null, null, savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = saveFlattenedAssignment(flattenedAssignment);

        testEntityManager.flush();
        testEntityManager.clear();

        Page<ResourceAssignmentUser> resourceAssignmentUsers =
                assignmentUserService.findResourceAssignmentUsersForResourceId(1L, "ALLTYPES", List.of("555"), List.of("555"), null, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        ResourceAssignmentUser resourceAssignmentUser = resourceAssignmentUsers.getContent().get(0);
        assertThat(resourceAssignmentUser.getAssignmentRef()).isEqualTo(savedFlattenedAssignment.getAssignmentId());
        assertThat(resourceAssignmentUser.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(resourceAssignmentUser.getAssignmentViaRoleRef()).isEqualTo(savedAssignment.getRoleRef());
        assertThat(resourceAssignmentUser.isDirectAssignment()).isFalse();

        assertThat(resourceAssignmentUser.getAssignmentViaRoleName()).isNotEmpty();
        assertThat(resourceAssignmentUser.getAssignmentViaRoleName()).isEqualTo(savedRole.getRoleName());

        assertThat(resourceAssignmentUser.getAssigneeRef()).isEqualTo(savedRole.getId());
        assertThat(resourceAssignmentUser.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(resourceAssignmentUser.getAssigneeFirstName()).isEqualTo("Test");
        assertThat(resourceAssignmentUser.getAssigneeLastName()).isEqualTo("Testesen");
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_userFilter() {
        Resource savedResource = createResource(1L, "1", "ALLTYPES", "Test resource");
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no",  userUUID1, "555", "ALLTYPES");
        User savedUser2 = createUser(456L, "Test2", "Testesen2",  "test2@test.no", userUUID2, "456" , "ALLTYPES");
        Role savedRole = createRole();
        Assignment savedAssignment = createAssignment(savedRole.getId(), null, null, savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = saveFlattenedAssignment(flattenedAssignment);

        FlattenedAssignment flattenedAssignment2 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser2.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment2 = saveFlattenedAssignment(flattenedAssignment2);

        testEntityManager.flush();
        testEntityManager.clear();

        Page<ResourceAssignmentUser> resourceAssignmentUsers =
                assignmentUserService.findResourceAssignmentUsersForResourceId(1L, "ALLTYPES", List.of("456"), List.of("456"), List.of(456L), null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        ResourceAssignmentUser resourceAssignmentUser = resourceAssignmentUsers.getContent().get(0);
        assertThat(resourceAssignmentUser.getAssignmentRef()).isEqualTo(savedFlattenedAssignment.getAssignmentId());
        assertThat(resourceAssignmentUser.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(resourceAssignmentUser.getAssignmentViaRoleRef()).isEqualTo(savedAssignment.getRoleRef());
        assertThat(resourceAssignmentUser.isDirectAssignment()).isFalse();

        assertThat(resourceAssignmentUser.getAssignmentViaRoleName()).isNotEmpty();
        assertThat(resourceAssignmentUser.getAssignmentViaRoleName()).isEqualTo(savedRole.getRoleName());

        assertThat(resourceAssignmentUser.getAssigneeRef()).isEqualTo(savedUser2.getId());
        assertThat(resourceAssignmentUser.getAssignerDisplayname()).isEqualTo(savedUser.getDisplayname());
        assertThat(resourceAssignmentUser.getAssigneeFirstName()).isEqualTo(savedUser2.getFirstName());
        assertThat(resourceAssignmentUser.getAssigneeLastName()).isEqualTo(savedUser2.getLastName());
    }

    @Test
    void findResourceAssignmentUsersForResourceId_shouldAllowDeletionForRestrictedResourceInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

        User savedUser = userRepository.saveAndFlush(studentUser);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
                .entraUserId(savedUser.getIdentityProviderUserObjectId())
                .resourceRef(savedResourceAdobek12.getId())
                .applicationResourceLocationOrgUnitId(kompavd)
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        saveFlattenedAssignment(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<ResourceAssignmentUser> resourceAssignmentUsers
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                1L,
                allTypes,
                kompavdList,
                kompavdList,
                null,
                null,
                0,
                20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }

    @Test
    void findResourceAssignmentUsersForResourceId_shouldDenyDeletionForRestrictedResourceOutOfScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

//        User user = User.builder()
//                .id(123L)
//                .firstName("Test")
//                .lastName("Testesen")
//                .userName("test")
//                .organisationUnitId(kompavd)
//                .userType(student)
//                .build();

        User savedUser = userRepository.saveAndFlush(studentUser);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
                .resourceRef(savedResourceAdobek12.getId())
                .applicationResourceLocationOrgUnitId(varfk)
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        saveFlattenedAssignment(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<ResourceAssignmentUser> resourceAssignmentUsers
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                1L,
                allTypes,
                kompavdList,
                kompavdList,
                null,
                null,
                0,
                20);
        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isFalse();
    }

    @Test
    void findResourceAssignmentUsersForResourceId_shouldAllowDeletionForUnrestrictedResourceOutOfScope() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId(zip)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.FREEALL.name())
                .build();

        Resource savedResource = resourceRepository.saveAndFlush(resource);

//        User user = User.builder()
//                .id(123L)
//                .firstName("Test")
//                .lastName("Testesen")
//                .userName("test")
//                .organisationUnitId(kompavd)
//                .userType(student)
//                .build();

        User savedUser = userRepository.saveAndFlush(studentUser);

        Assignment assignment = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
                .entraUserId(savedUser.getIdentityProviderUserObjectId())
                .resourceRef(savedResource.getId())
                .applicationResourceLocationOrgUnitId(varfk)
                .build();

        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResource.getId())
                .build();

        saveFlattenedAssignment(flattenedAssignment);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<ResourceAssignmentUser> resourceAssignmentUsers
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                2L,
                allTypes,
                kompavdList,
                kompavdList,
                null,
                null,
                0,
                20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }

    @Test
    void findResourceAssignmentUsersForResourceId_shouldDenyDeletionForRestrictedResourceWithoutLocation() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId(kompavd)
                .userType(student)
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        saveFlattenedAssignment(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<ResourceAssignmentUser> resourceAssignmentUsers
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                1L,
                allTypes,
                kompavdList,
                kompavdList,
                null,
                null,
                0,
                20);
        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isFalse();
    }
    @Test
    void findResourceAssignmentUsersForResourceId_shouldAllowDeletionWhenAllOrgUnitsAreInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

//        User user = User.builder()
//                .id(123L)
//                .firstName("Test")
//                .lastName("Testesen")
//                .userName("test")
//                .organisationUnitId(kompavd)
//                .userType(student)
//                .build();

        User savedUser = userRepository.saveAndFlush(studentUser);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
                .entraUserId(savedUser.getIdentityProviderUserObjectId())
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        saveFlattenedAssignment(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(allorgunitsList);

        Page<ResourceAssignmentUser> resourceAssignmentUsers
                = assignmentUserService.findResourceAssignmentUsersForResourceId(
                1L,
                allTypes,
                allorgunitsList,
                allorgunitsList,
                null,
                null,
                0,
                20);
        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }
    private Resource createResource(Long id, String resourceId, String resourceType, String resourceName) {
        Resource resource = Resource.builder()
                .id(id)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .resourceName(resourceName)
                .build();

        return resourceRepository.saveAndFlush(resource);
    }

    private Assignment createAssignment(Long roleId, Long userId, UUID azureUserId, Long resourceId, String assignerUserName, Date assignmentRemovedDate) {
        Assignment assignment = Assignment.builder()
                .assignerUserName(assignerUserName)
                .assignmentRemovedDate(assignmentRemovedDate)
                .roleRef(roleId)
                .userRef(userId)
                .entraUserId(azureUserId)
                .resourceRef(resourceId)
                .build();
        return assignmentRepository.saveAndFlush(assignment);
    }

    private Role createRole() {
        Role role = Role.builder()
                .id(123L)
                .roleName("Test role")
                .organisationUnitName("Test org unit")
                .organisationUnitId("555")
                .roleType("ALLTYPES")
                .build();

        return roleRepository.saveAndFlush(role);
    }

    private FlattenedAssignment saveFlattenedAssignment(FlattenedAssignment flattenedAssignment) {
        UserEntraMembership membership = userEntraMembershipRepository.saveAndFlush(UserEntraMembership.builder()
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build());

        flattenedAssignment.setIdentityProviderUserObjectId(membership.getUserEntraId());
        flattenedAssignment.setIdentityProviderGroupObjectId(membership.getResourceEntraId());
        flattenedAssignment.setUserEntraMembership(membership);
        return flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);
    }

    private User createUser(long id, String firstName, String lastName, String userName, UUID identityProviderUserObjectId, String orgId, String userType) {
        User user = User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .organisationUnitId(orgId)
                .userName(userName)
                .identityProviderUserObjectId(identityProviderUserObjectId)
                .userType(userType)
                .status("ACTIVE")
                .build();

        return userRepository.saveAndFlush(user);
    }
}
