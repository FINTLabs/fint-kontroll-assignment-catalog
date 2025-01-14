package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationResourceLocation.ApplicationResourceLocationService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.AssignmentUserService;
import no.fintlabs.user.AssignmentUser;
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

    @Test
    public void shouldNotFindUsersWithDeletedAssignments() {
        Resource resource = Resource.builder()
                .id(2L)
                .resourceId("2")
                .resourceType("ALLTYPES")
                .resourceName("Test resource")
                .build();

        resourceRepository.save(resource);

        Resource resource2 = Resource.builder()
                .id(3L)
                .resourceId("3")
                .resourceType("ALLTYPES")
                .resourceName("Test resource 2")
                .build();

        resourceRepository.save(resource2);

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
                .resourceRef(2L)
                .build();

        assignmentRepository.save(assignment);

        Assignment assignment2 = Assignment.builder()
                .assignmentId("456")
                .assignerUserName("not deleted")
                .assignmentRemovedDate(null)
                .userRef(123L)
                .resourceRef(3L)
                .build();

        assignmentRepository.save(assignment2);


        Specification<User> spec = new UserSpecificationBuilder(2L, "ALLTYPES", List.of("555"), List.of("555"), null)
                .assignmentSearch();

        Page<AssignmentUser> usersPage = assignmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void shouldFindUsersWithAssignments() {
        Resource resource = Resource.builder()
                .id(2L)
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

        Assignment assignmentNotDeleted = Assignment.builder()
                .assignmentId("456")
                .assignerUserName("not-deleted")
                .assignmentRemovedDate(null)
                .userRef(123L)
                .resourceRef(2L)
                .build();
        assignmentRepository.save(assignmentNotDeleted);


        Specification<User> spec = new UserSpecificationBuilder(2L, "ALLTYPES", List.of("555"), List.of("555"), null)
                .assignmentSearch();

        Page<AssignmentUser> usersPage = assignmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(1);
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
                .userRef(savedUser.getId())
                .roleRef(null)
                .resourceRef(savedResource.getId())
                .build();
        Assignment savedAssignment = assignmentRepository.saveAndFlush(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(savedUser.getId())
                .resourceRef(savedResource.getId())
                .build();
        FlattenedAssignment savedFlattenedAssignment = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment);

        testEntityManager.flush();
        testEntityManager.clear();

        Page<ResourceAssignmentUser> resourceAssignmentUsers =
                assignmentUserService.findResourceAssignmentUsers(1L, "ALLTYPES", List.of("555"), List.of("555"), null, 0, 20);

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
                assignmentUserService.findResourceAssignmentUsers(1L, "ALLTYPES", List.of("555"), List.of("555"), null, 0, 20);

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
}
