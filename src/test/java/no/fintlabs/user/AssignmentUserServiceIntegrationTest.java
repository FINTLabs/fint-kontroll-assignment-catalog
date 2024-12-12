package no.fintlabs.user;

import jakarta.transaction.Transactional;
import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
@Import({AssignmentUserService.class})
public class AssignmentUserServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private AssignmentUserService assignmentUserService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @MockBean
    private AssignmentService assignmentService;

    User user = User.builder()
            .id(1L)
            .firstName("Jan Anders")
            .lastName("Hansen")
            .userType("EMPLOYEESTAFF")
            .build();

    List<String> allOrgUnitsAsList = List.of(OrgUnitType.ALLORGUNITS.name());

    Resource resource = Resource.builder()
            .id(1L)
            .resourceName("Adobek12")
            .build();

    Assignment assignment = Assignment.builder()
            .id(1L)
            .userRef(1L)
            .resourceRef(1L)
            .assignmentRemovedDate(null)
            .build();
    FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
            .id(1L)
            .assignmentId(1L)
            .userRef(1L)
            .resourceRef(1L)
            .build();

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        resourceRepository.deleteAll();
        assignmentRepository.deleteAll();
        flattenedAssignmentRepository.deleteAll();
        userRepository.save(user);
        resourceRepository.save(resource);
        assignmentRepository.save(assignment);
        flattenedAssignmentRepository.save(flattenedAssignment);
    }
    @Test
    void givenUserWithMiddlename_whenSearchStringContainsMiddleName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsers(
                1L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                "Anders",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenUserWithMiddlename_whenSearchStringDoesNotContainsMiddleName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsers(
                1L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                "Jan Hansen",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenUser_whenSearchStringContainsPartOfLastName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsers(
                1L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                "Hans",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
}
