package no.fintlabs.role;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
@Import({AssignmentRoleService.class, AssignmentService.class, FlattenedAssignmentService.class})
public class AssignmentRoleServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentRoleService assignmentRoleService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @MockBean
    private OpaService opaService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void shouldFindRoleResourcesNotDeleted() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("1")
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

        Role role = Role.builder()
                .id(1L)
                .roleType("ALLTYPES")
                .roleName("test role")
                .organisationUnitId("555")
                .organisationUnitName("test orgunit")
                .build();
        roleRepository.save(role);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("not-deleted")
                .assignmentRemovedDate(null)
                .userRef(123L)
                .resourceRef(1L)
                .roleRef(1L)
                .build();
        assignmentRepository.save(assignment);


        RoleSpecificationBuilder builder = new RoleSpecificationBuilder(1L, "ALLTYPES", List.of("555"), List.of("555"), null);
        Page<AssignmentRole> usersPage = assignmentRoleService.findBySearchCriteria(1L, builder.build(), Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void shouldNotFindRoleResourcesDeleted() {
        Resource resource = Resource.builder()
                .id(1L)
                .resourceId("1")
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

        Role role = Role.builder()
                .id(1L)
                .roleType("ALLTYPES")
                .roleName("test role")
                .organisationUnitId("555")
                .organisationUnitName("test orgunit")
                .build();
        roleRepository.save(role);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("deleted")
                .assignmentRemovedDate(new Date())
                .userRef(123L)
                .resourceRef(1L)
                .roleRef(1L)
                .build();
        assignmentRepository.save(assignment);


        RoleSpecificationBuilder builder = new RoleSpecificationBuilder(1L, "ALLTYPES", List.of("555"), List.of("555"), null);
        Page<AssignmentRole> usersPage = assignmentRoleService.findBySearchCriteria(1L, builder.build(), Pageable.unpaged());

        assertThat(usersPage.getTotalElements()).isEqualTo(0);
    }

}
