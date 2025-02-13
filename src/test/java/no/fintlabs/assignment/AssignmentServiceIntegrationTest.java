package no.fintlabs.assignment;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.AuthorizationClient;
import no.fintlabs.opa.OpaApiClient;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import no.fintlabs.util.AuthenticationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({AssignmentService.class, FlattenedAssignmentService.class, OpaService.class, AuthorizationClient.class, OpaApiClient.class,
        RestTemplate.class, AuthenticationUtil.class, FlattenedAssignmentMapper.class, FlattenedAssignmentMembershipService.class, AssigmentEntityProducerService.class})
@Testcontainers
public class AssignmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerService;

    @MockBean
    private OpaService opaService;

    @MockBean
    private ApplicationResourceLocationService applicationResourceLocationService;

    @Autowired
    private AuthorizationClient authorizationClient;

    @Autowired
    private OpaApiClient opaApiClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuthenticationUtil authenticationUtil;

    @Autowired
    private AssignmentService assignmentService;

    @Test
    public void shouldGetAssignmentsNotDeleted() {
        Assignment activeAssignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("not-deleted")
                .assignmentRemovedDate(null).build();

        Assignment deletedAssignment = Assignment.builder()
                .assignmentId("456")
                .assignerUserName("deleted")
                .assignmentRemovedDate(new Date()).build();

        assignmentRepository.save(activeAssignment);
        assignmentRepository.save(deletedAssignment);

        List<SimpleAssignment> simpleAssignments = assignmentService.getSimpleAssignments();
        assertThat(simpleAssignments).hasSize(1);
        assertTrue(simpleAssignments.stream().allMatch(simpleAssignment -> simpleAssignment.getAssignerUsername().equals("not-deleted")));
    }

    @Transactional
    @Test
    public void shouldDeleteAssignment() {
        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .assignerUserName("not-deleted")
                .assignmentRemovedDate(null).build();

        Assignment assignmentForDeletion = assignmentRepository.save(assignment);

        when(opaService.getUserNameAuthenticatedUser()).thenReturn("not-deleted");
        when(userRepository.getUserByUserName("not-deleted")).thenReturn(
                Optional.of(User.builder().id(1L).userName("not-deleted").build()));

        Assignment deletedAss = assignmentService.deleteAssignment(assignmentForDeletion.getId());

        assertThat(deletedAss.getAssignmentRemovedDate()).isNotNull();
        assertThat(deletedAss.getAssignerRemoveRef()).isNotNull();

        List<SimpleAssignment> simpleAssignments = assignmentService.getSimpleAssignments();

        assertThat(simpleAssignments).isEmpty();
    }

    @Transactional
    @Test
    public void shouldUpdateAssignmentMissingResourceConsumerWhenNearestResourceConsumerExists () {

        Resource resource = Resource.builder()
                .id(1L)
                .resourceName("Adobe Creative Cloud")
                .build();

        resourceRepository.save(resource);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .resourceRef(1L)
                .organizationUnitId("realfagavd")
                .build();

        Long assignmentId = assignmentRepository.save(assignment).getId();

        when(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "realfagavd"))
                .thenReturn(Optional.of(new NearestResourceLocationDto("vgmidt", "Vg midt")));

        assignmentService.updateAssignmentsWithApplicationResourceLocationOrgUnit(Set.of(assignmentId));
        Assignment updatedAssignment = assignmentRepository.findById(assignmentId).get();

        assertEquals("vgmidt", updatedAssignment.getApplicationResourceLocationOrgUnitId());
        assertEquals("Vg midt", updatedAssignment.getApplicationResourceLocationOrgUnitName());
    }

    @Test
    public void shouldNotUpdateAssignmentMissingResourceConsumerWhenNoResourceConsumerExists () {

        Resource resource = Resource.builder()
                .id(2L)
                .resourceName("Adobe Creative Cloud")
                .build();

        resourceRepository.saveAndFlush(resource);

        Assignment assignment = Assignment.builder()
                .assignmentId("123")
                .resourceRef(2L)
                .organizationUnitId("realfagavd")
                .build();

        Long assignmentId = assignmentRepository.save(assignment).getId();

        when(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "realfagavd"))
                .thenReturn(Optional.empty());

        assignmentService.updateAssignmentsWithApplicationResourceLocationOrgUnit(Set.of(assignmentId));
        Assignment updatedAssignment = assignmentRepository.findById(assignmentId).get();

        assertNull(updatedAssignment.getApplicationResourceLocationOrgUnitId());
    }
}



