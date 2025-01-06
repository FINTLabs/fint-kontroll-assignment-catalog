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
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        testEntityManager.flush();
        testEntityManager.clear();

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
}
