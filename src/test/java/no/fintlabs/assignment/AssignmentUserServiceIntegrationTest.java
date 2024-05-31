package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.AssigmentUserService;
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
@Import({AssigmentUserService.class, AssignmentService.class, FlattenedAssignmentService.class, FlattenedAssignmentMapper.class, FlattenedAssignmentMembershipService.class})
public class AssignmentUserServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssigmentUserService assigmentUserService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;

    @MockBean
    private OpaService opaService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ResourceRepository resourceRepository;

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

        Page<AssignmentUser> usersPage = assigmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

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

        Page<AssignmentUser> usersPage = assigmentUserService.findBySearchCriteria(2L, spec, Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(1);
    }

    @Transactional
    @Test
    public void shouldFindResourceAssignmentUsers() {
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
                .userType("ALLTYPES")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        Assignment assignment = Assignment.builder()
                .assignmentId("456")
                .assignerUserName("test@test.no")
                .assignmentRemovedDate(null)
                .userRef(savedUser.getId())
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
                assigmentUserService.findResourceAssignmentUsers(1L, "ALLTYPES", List.of("555"), List.of("555"), null, 0, 20);

        assertThat(resourceAssignmentUsers.getTotalElements()).isEqualTo(1);

    }
}
