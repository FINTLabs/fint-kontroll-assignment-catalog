package no.fintlabs.resource;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.AssigmentEntityProducerService;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
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
}
