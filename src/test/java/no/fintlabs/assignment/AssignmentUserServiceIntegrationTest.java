package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.authorization.AuthorizationUtil;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.kodeverk.ScopeType;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.AssignmentUser;
import no.fintlabs.user.AssignmentUserService;
import no.fintlabs.user.ResourceAssignmentUser;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import no.fintlabs.user.UserSpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Testcontainers
@Import({AssignmentUserService.class, AssignmentService.class, FlattenedAssignmentService.class, FlattenedAssignmentMapper.class, FlattenedAssignmentMembershipService.class, AssigmentEntityProducerService.class})
public class AssignmentUserServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentUserService assignmentUserService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @MockBean
    private OpaService opaService;
    @MockBean
    private AuthorizationUtil authorizationUtil;

    @MockBean
    private ApplicationResourceLocationService applicationResourceLocationService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

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

    @Test
    public void shouldNotFindUsersWithDeletedAssignments() {
        Resource resource = createResource(2L, "2", "ALLTYPES", "Test resource");
        Resource resource2 = createResource(3L, "3", "ALLTYPES", "Test resource 2");
        User user = createUser(123L, "Test", "Testesen", "test", "555", "ALLTYPES");
        Assignment assignment = createAssignment(null, user.getId(), resource.getId(), "deleted", new Date());
        Assignment assignment2 = createAssignment(null, user.getId(), resource2.getId(), "not deleted", null);

        Specification<User> spec = new UserSpecificationBuilder(2L, "ALLTYPES", List.of("555"), List.of("555"), null)
                .assignmentSearch();

        Page<AssignmentUser> usersPage = assignmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void shouldFindUsersWithAssignments() {
        Resource resource = createResource(2L, "2", "ALLTYPES", "Test resource");
        User user = createUser(123L, "Test", "Testesen", "test", "555", "ALLTYPES");
        Assignment assignmentNotDeleted = createAssignment(null, user.getId(), resource.getId(), "not deleted", null);

        Specification<User> spec = new UserSpecificationBuilder(2L, "ALLTYPES", List.of("555"), List.of("555"), null)
                .assignmentSearch();

        Page<AssignmentUser> usersPage = assignmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(1);
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_user_direct() {
        Resource savedResource = createResource(1L, "1", "ALLTYPES", "Test resource");
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no", "555", "ALLTYPES");
        Assignment savedAssignment = createAssignment(null, savedUser.getId(), savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

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
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no", "555", "ALLTYPES");
        Role savedRole = createRole();
        Assignment savedAssignment = createAssignment(savedRole.getId(), null, savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

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
        User savedUser = createUser(123L, "Test", "Testesen", "test@test.no", "555", "ALLTYPES");
        User savedUser2 = createUser(456L, "Test2", "Testesen2",  "test2@test.no", "456" , "ALLTYPES");
        Role savedRole = createRole();
        Assignment savedAssignment = createAssignment(savedRole.getId(), null, savedResource.getId(), "test@test.no", null);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        FlattenedAssignment flattenedAssignment2 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser2.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment2 = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment2);

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
    public void shouldSetIsDeletableAssignmentToTrueForRestrictedResourceWhenCalledByUserAndApplicationResourceLocationOrgUnitIdIsInScope() {
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
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
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

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

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
    public void shouldSetIsDeletableAssignmentToFalseWhenCalledByUserAndRestrictedResourceNotInScope() {
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
                .build();

        User savedUser = userRepository.saveAndFlush(user);

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

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

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
    public void shouldSetIsDeletableAssignmentToTrueWhenCalledByUserAndUnrestrictedResourceNotInScope() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId(zip)
                .resourceType(allTypes)
                .licenseEnforcement(Handhevingstype.FREEALL.name())
                .build();

        Resource savedResource = resourceRepository.saveAndFlush(resource);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId(kompavd)
                .userType(student)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Assignment assignment = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
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

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

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
    public void shouldSetIsDeletableAssignmentToFalseWhenCalledByUserAndRestrictedResourceNotInScopeWithNoApplicationResourceLocation() {
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

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

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
    public void shouldSetIsDeletableAssignmentToTrueWhenCalledByUserWithAllOrgUnitsInScopeWithNoApplicationResourceLocation() {
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

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

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

    private Assignment createAssignment(Long roleId, Long userId, Long resourceId, String assignerUserName, Date assignmentRemovedDate) {
        Assignment assignment = Assignment.builder()
                .assignerUserName(assignerUserName)
                .assignmentRemovedDate(assignmentRemovedDate)
                .roleRef(roleId)
                .userRef(userId)
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

    private User createUser(long id, String firstName, String lastName, String userName, String orgId, String userType) {
        User user = User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .organisationUnitId(orgId)
                .userName(userName)
                .userType(userType)
                .build();

        return userRepository.saveAndFlush(user);
    }
}
