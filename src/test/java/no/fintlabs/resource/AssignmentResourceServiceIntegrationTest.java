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

    @Autowired
    private RoleRepository roleRepository;

    private final String varfk = "varfk";
    private final String kompavd = "kompavd";

    private final List<String> kompavdList = List.of(kompavd);

    private final String zip = "zip";
    private final String kabal = "kabal";
    private final String adobek12 = "adobek12";
    private final String m365 = "m365";

    private final String student = "Student";
    private final String freeAll = "FREE-ALL";
    private final String freeStudent = "FREE-STUDENT";
    private final String hardStop = "HARDSTOP";
    private final String allTypes = "ALLTYPES";

    @Test
    public void shouldSetIsDeletableAssignmentToTrueForRestrictedResourceWhenResourceConsumerOrgUnitIdIsInScope() {
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
    public void shouldSetIsDeletableAssignmentToFalseForRestrictedResourceNotInScope() {
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
    public void shouldSetIsDeletableAssignmentToTrueForUnrestrictedResourceNotInScope() {
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

        Assignment assignment = Assignment.builder()
                .roleRef(null)
                .userRef(savedUser.getId())
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

        Page<UserAssignmentResource> resourceAssignmentUsers =
                assignmentResourceService.findUserAssignmentResourcesByUser(123L, "ALLTYPES", List.of("555"), List.of("555"), null, 0, 20);

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
}
