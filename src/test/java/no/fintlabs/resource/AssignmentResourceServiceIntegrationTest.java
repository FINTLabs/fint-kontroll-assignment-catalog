package no.fintlabs.resource;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.authorization.AuthorizationUtil;
import no.fintlabs.opa.OpaService;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Testcontainers
@Import({AssignmentResourceService.class, AssignmentService.class, FlattenedAssignmentService.class, FlattenedAssignmentMapper.class, FlattenedAssignmentMembershipService.class, AssigmentEntityProducerService.class})
public class AssignmentResourceServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentResourceService assignmentResourceService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @MockBean
    private OpaService opaService;
    @MockBean
    private AuthorizationUtil authorizationUtil;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FlattenedAssignmentMapper flattenedAssignmentMapper;

    @Autowired
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private final String varfk = "varfk";
    private final String kompavd = "kompavd";

    private final List<String> kompavdList = List.of(kompavd);

    private final String zip = "zip";
    private final String adobek12 = "adobek12";
    private final String student = "Student";
    private final String freeAll = "FREE-ALL";
    private final String hardStop = "HARDSTOP";
    private final String allTypes = "ALLTYPES";

    @Test
    public void shouldSetIsDeletableAssignmentToTrueForRestrictedResourceWhenCalledByRoleAndResourceConsumerOrgUnitIdIsInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(hardStop)
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

        Role role = Role.builder()
                .id(1L)
                .roleType(allTypes)
                .roleName("Test role")
                .organisationUnitId(kompavd)
                .build();

        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(null)
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResourceAdobek12.getId())
                .resourceConsumerOrgUnitId(kompavd)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByRole(1L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }
    @Test
    public void shouldSetIsDeletableAssignmentToFalseWhenCalledByRoleAndRestrictedResourceNotInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(hardStop)
                .build();

        Resource savedResourceAdobek12 = resourceRepository.saveAndFlush(resourceAdobek12);

        Role role = Role.builder()
                .id(1L)
                .roleType(allTypes)
                .roleName("Test role")
                .organisationUnitId(kompavd)
                .build();

        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignmentAdobek12 = Assignment.builder()
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResourceAdobek12.getId())
                .build();

        Assignment savedAssignmentAdobek12 = assignmentRepository.saveAndFlush(assignmentAdobek12);

        FlattenedAssignment flattenedAssignmentAdobek12 = FlattenedAssignment.builder()
                .assignmentId(savedAssignmentAdobek12.getId())
                .userRef(null)
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResourceAdobek12.getId())
                .resourceConsumerOrgUnitId(varfk)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByRole(1L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isFalse();
    }
    @Test
    public void shouldSetIsDeletableAssignmentToTrueWhenCalledByRoleAndUnrestrictedResourceNotInScope() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId(zip)
                .resourceType(allTypes)
                .licenseEnforcement(freeAll)
                .build();
        Resource savedResource = resourceRepository.saveAndFlush(resource);

        Role role = Role.builder()
                .id(1L)
                .roleType(allTypes)
                .roleName("Test role")
                .organisationUnitId(kompavd)
                .build();
        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignment = Assignment.builder()
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResource.getId())
                .build();
        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedRole.getId())
                .assignmentViaRoleRef(1L)
                .resourceRef(savedResource.getId())
                .resourceConsumerOrgUnitId(varfk)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByRole(1L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }
    @Test
    public void shouldSetIsDeletableAssignmentToTrueForRestrictedResourceWhenCalledByUserAndResourceConsumerOrgUnitIdIsInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(hardStop)
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
                .resourceConsumerOrgUnitId(kompavd)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);



        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }

    @Test
    public void shouldSetIsDeletableAssignmentToFalseWhenCalledByUserAndRestrictedResourceNotInScope() {
        Resource resourceAdobek12 = Resource.builder()
                .id(1L)
                .resourceId(adobek12)
                .resourceType(allTypes)
                .licenseEnforcement(hardStop)
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
                .resourceConsumerOrgUnitId(varfk)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentAdobek12);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isFalse();
    }
    @Test
    public void shouldSetIsDeletableAssignmentToTrueWhenCalledByUserAndUnrestrictedResourceNotInScope() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId(zip)
                .resourceType(allTypes)
                .licenseEnforcement(freeAll)
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
                .build();

        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResource.getId())
                .resourceConsumerOrgUnitId(varfk)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isTrue();
    }
    @Test
    public void shouldSetIsDeletableAssignmentToFalseForUnrestrictedResourceNotInScopeAndIndirectlyAssigned() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId(zip)
                .resourceType(allTypes)
                .licenseEnforcement(freeAll)
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

        Role role = Role.builder()
                .id(1L)
                .roleType(allTypes)
                .roleName("Test role")
                .organisationUnitId(kompavd)
                .build();
        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignment = Assignment.builder()
                .roleRef(role.getId())
                .userRef(null)
                .resourceRef(savedResource.getId())
                .build();

        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(1L)
                .resourceRef(savedResource.getId())
                .resourceConsumerOrgUnitId(varfk)
                .build();

        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(kompavdList);

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, allTypes, kompavdList, kompavdList, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        assertThat(resourceAssignmentUsers.getContent().getFirst().isDeletableAssignment()).isFalse();
    }
    @Test
    public void shouldFindUserResourcesNotDeleted() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("2")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .build();

        resourceRepository.save(resource);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId("555")
                .userType("ALLTYPES")
                .build();

        userRepository.save(user);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("not-deleted")
                .assignmentRemovedDate(null)
                .userRef(123L)
                .resourceRef(1L)
                .build();
        assignmentRepository.save(assignment);


        ResourceSpecificationBuilder builder = new ResourceSpecificationBuilder(123L, null, "ALLTYPES", null, null, null);
        Page<AssignmentResource> usersPage = assignmentResourceService.getResourcesAssignedToUser(123L, builder.build(), Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void shouldNotFindUserResourcesDeleted() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("2")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .build();

        resourceRepository.save(resource);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId("555")
                .userType("ALLTYPES")
                .build();

        userRepository.save(user);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("deleted")
                .assignmentRemovedDate(new Date())
                .userRef(123L)
                .resourceRef(1L)
                .build();
        assignmentRepository.save(assignment);


        ResourceSpecificationBuilder builder = new ResourceSpecificationBuilder(123L, null, "ALLTYPES", null, null, null);
        Page<AssignmentResource> usersPage = assignmentResourceService.getResourcesAssignedToUser(123L, builder.build(), Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(0);
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_user_direct() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("1")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .licenseEnforcement(freeAll)
                .build();

        Resource savedResource = resourceRepository.saveAndFlush(resource);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId("555")
                .userName("test@test.no")
                .userType("ALLTYPES")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Assignment assignment = Assignment.builder()
                .assignerUserName("test@test.no")
                .assignmentRemovedDate(null)
                .roleRef(null)
                .userRef(savedUser.getId())
                .resourceRef(savedResource.getId())
                .build();
        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(null)
                .resourceRef(savedResource.getId())
                .resourceConsumerOrgUnitId(varfk)
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        testEntityManager.flush();
        testEntityManager.clear();
        given(authorizationUtil.getAllAuthorizedOrgUnitIDs()).willReturn(List.of("555"));

        List<Long> resourceIds = List.of(savedResource.getId());

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, "ALLTYPES", List.of("555"), List.of("555"), resourceIds, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);
        UserAssignmentResource userAssignmentResource = resourceAssignmentUsers.getContent().get(0);

        assertThat(userAssignmentResource.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(userAssignmentResource.getAssignmentRef()).isEqualTo(savedFlattenedAssignment.getAssignmentId());
        assertThat(userAssignmentResource.isDirectAssignment()).isTrue();
        assertThat(userAssignmentResource.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(userAssignmentResource.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(userAssignmentResource.getResourceRef()).isEqualTo(savedResource.getId());
        assertThat(userAssignmentResource.getResourceName()).isEqualTo("Test resource");
        assertThat(userAssignmentResource.getResourceType()).isEqualTo("ALLTYPES");
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUser_user_indirect_filter_resourceid() {
        Resource resource = Resource.builder()
                .id(111L)
                .resourceId("111")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .build();

        Resource savedResourceIndirect = resourceRepository.saveAndFlush(resource);

        Resource resource2 = Resource.builder()
                .id(222L)
                .resourceId("222")
                .resourceType("ALLTYPES")
                .resourceName("Test resource 222")
                .build();

        Resource savedResourceDirect = resourceRepository.saveAndFlush(resource2);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId("555")
                .userName("test@test.no")
                .userType("ALLTYPES")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Role role = Role.builder()
                .id(123L)
                .roleName("Test role")
                .organisationUnitName("Test org unit")
                .organisationUnitId("555")
                .roleType("ALLTYPES")
                .build();

        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignment = Assignment.builder()
                .assignerUserName("test@test.no")
                .assignmentRemovedDate(null)
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResourceIndirect.getId())
                .build();
        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignmentIndirect = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResourceIndirect.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignmentInDirect = flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentIndirect);

        FlattenedAssignment flattenedAssignmentDirect = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .resourceRef(savedResourceIndirect.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignmentDirect = flattenedAssignmentRepository.saveAndFlush(flattenedAssignmentDirect);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> resourceIds = List.of(savedResourceIndirect.getId(), savedResourceDirect.getId());

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, "ALLTYPES", List.of("555"), List.of("555"), resourceIds, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(2);
        UserAssignmentResource foundResourceIndirect = resourceAssignmentUsers.getContent().get(0);
        UserAssignmentResource foundResourceDirect = resourceAssignmentUsers.getContent().get(1);

        assertThat(foundResourceIndirect.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(foundResourceIndirect.getAssignmentRef()).isEqualTo(savedFlattenedAssignmentInDirect.getAssignmentId());
        assertThat(foundResourceIndirect.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(foundResourceIndirect.getAssignmentViaRoleRef()).isEqualTo(savedAssignment.getRoleRef());
        assertThat(foundResourceIndirect.isDirectAssignment()).isFalse();

        assertThat(foundResourceIndirect.getAssignmentViaRoleName()).isNotEmpty();
        assertThat(foundResourceIndirect.getAssignmentViaRoleName()).isEqualTo(savedRole.getRoleName());

        assertThat(foundResourceIndirect.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(foundResourceIndirect.getResourceRef()).isEqualTo(savedResourceIndirect.getId());
        assertThat(foundResourceIndirect.getResourceName()).isEqualTo("Test resource");
        assertThat(foundResourceIndirect.getResourceType()).isEqualTo("ALLTYPES");

        assertThat(foundResourceDirect.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(foundResourceDirect.getAssignmentRef()).isEqualTo(savedFlattenedAssignmentDirect.getAssignmentId());
        assertThat(foundResourceDirect.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(foundResourceDirect.getAssignmentViaRoleRef()).isNull();
        assertThat(foundResourceDirect.isDirectAssignment()).isTrue();

        assertThat(foundResourceDirect.getAssignmentViaRoleName()).isNull();

        assertThat(foundResourceDirect.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(foundResourceDirect.getResourceRef()).isEqualTo(savedResourceIndirect.getId());
        assertThat(foundResourceDirect.getResourceName()).isEqualTo("Test resource");
        assertThat(foundResourceDirect.getResourceType()).isEqualTo("ALLTYPES");

    }

    @Transactional
    @Test
    public void shouldFindUserAssignmentResourcesByRole_user_indirect_filter_resourceid() {
        Resource resource = Resource.builder()
                .id(111L)
                .resourceId("111")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .build();

        Resource savedResource = resourceRepository.saveAndFlush(resource);

        Resource resource2 = Resource.builder()
                .id(222L)
                .resourceId("222")
                .resourceType("ALLTYPES")
                .resourceName("Test resource 222")
                .build();

        Resource savedResource2 = resourceRepository.saveAndFlush(resource2);

        User user = User.builder()
                .id(123L)
                .firstName("Test")
                .lastName("Testesen")
                .userName("test")
                .organisationUnitId("555")
                .userName("test@test.no")
                .userType("ALLTYPES")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Role role = Role.builder()
                .id(123L)
                .roleName("Test role")
                .organisationUnitName("Test org unit")
                .organisationUnitId("555")
                .roleType("ALLTYPES")
                .build();

        Role savedRole = roleRepository.saveAndFlush(role);

        Assignment assignment = Assignment.builder()
                .assignerUserName("test@test.no")
                .assignmentRemovedDate(null)
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResource.getId())
                .build();
        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        Assignment assignment2 = Assignment.builder()
                .assignerUserName("test@test.no")
                .assignmentRemovedDate(null)
                .roleRef(savedRole.getId())
                .userRef(null)
                .resourceRef(savedResource2.getId())
                .build();
        Assignment savedAssignment2 = assignmentRepository.saveAndFlush(assignment2);

        FlattenedAssignment flattenedAssignment1 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment1 = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment1);

        FlattenedAssignment flattenedAssignment2 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment2.getId())
                .userRef(savedUser.getId())
                .assignmentViaRoleRef(savedRole.getId())
                .resourceRef(savedResource2.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment2 = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment2);

        testEntityManager.flush();
        testEntityManager.clear();

        List<Long> resourceIds = List.of(savedResource.getId(), savedResource2.getId());

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByRole(123L, "ALLTYPES", List.of("555"), List.of("555"), resourceIds, null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(2);
        UserAssignmentResource foundResource1 = resourceAssignmentUsers.getContent().get(0);
        UserAssignmentResource foundResource2 = resourceAssignmentUsers.getContent().get(1);

        assertThat(foundResource1.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(foundResource1.getAssignmentRef()).isEqualTo(savedFlattenedAssignment1.getAssignmentId());
        assertThat(foundResource1.getAssignerUsername()).isEqualTo(savedAssignment.getAssignerUserName());
        assertThat(foundResource1.getAssignmentViaRoleRef()).isEqualTo(savedAssignment.getRoleRef());
        assertThat(foundResource1.isDirectAssignment()).isFalse();

        assertThat(foundResource1.getAssignmentViaRoleName()).isNotEmpty();
        assertThat(foundResource1.getAssignmentViaRoleName()).isEqualTo(savedRole.getRoleName());

        assertThat(foundResource1.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(foundResource1.getResourceRef()).isEqualTo(savedResource.getId());
        assertThat(foundResource1.getResourceName()).isEqualTo("Test resource");
        assertThat(foundResource1.getResourceType()).isEqualTo("ALLTYPES");

        assertThat(foundResource2.getAssigneeRef()).isEqualTo(savedUser.getId());
        assertThat(foundResource2.getAssignmentRef()).isEqualTo(savedFlattenedAssignment2.getAssignmentId());
        assertThat(foundResource2.getAssignerUsername()).isEqualTo(savedAssignment2.getAssignerUserName());
        assertThat(foundResource2.getAssignmentViaRoleRef()).isEqualTo(savedAssignment2.getRoleRef());
        assertThat(foundResource2.isDirectAssignment()).isFalse();

        assertThat(foundResource2.getAssignmentViaRoleName()).isNotEmpty();
        assertThat(foundResource2.getAssignmentViaRoleName()).isEqualTo(savedRole.getRoleName());

        assertThat(foundResource2.getAssignerDisplayname()).isEqualTo("Test Testesen");
        assertThat(foundResource2.getResourceRef()).isEqualTo(savedResource2.getId());
        assertThat(foundResource2.getResourceName()).isEqualTo("Test resource 222");
        assertThat(foundResource2.getResourceType()).isEqualTo("ALLTYPES");

    }
}
