package no.fintlabs.user;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import no.fintlabs.authorization.AuthorizationUtil;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
@Import({AssignmentUserService.class, AuthorizationUtil.class})
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
    @MockBean
    private AuthorizationUtil authorizationUtil;

    private List<String> allOrgUnitsAsList;
    private List<Long> userIds;


    @BeforeEach
    public void setUp() {
        // @DataJpaTest ruller tilbake alle transaksjoner etter hver test

//        userRepository.deleteAll();
        //        resourceRepository.deleteAll();
        //        assignmentRepository.deleteAll();
        //        flattenedAssignmentRepository.deleteAll();

        allOrgUnitsAsList = List.of(OrgUnitType.ALLORGUNITS.name());

        User user1 = User.builder()
                .id(123L)
                .firstName("Jan Anders")
                .lastName("Hansen")
                .userType("EMPLOYEESTAFF")
                .build();

        userRepository.save(user1);

        Resource resource = Resource.builder()
                .id(456L)
                .resourceName("Adobek12")
                .build();
        resourceRepository.save(resource);

        Assignment assignment = Assignment.builder()
                .userRef(123L)
                .resourceRef(456L)
                .assignmentRemovedDate(null)
                .build();
        Assignment savedAssignment = assignmentRepository.save(assignment);

        FlattenedAssignment flattenedAssignment = FlattenedAssignment.builder()
                .assignmentId(savedAssignment.getId())
                .userRef(123L)
                .resourceRef(456L)
                .build();

        flattenedAssignmentRepository.save(flattenedAssignment);
    }
    @Test
    void givenUserWithMiddlename_whenSearchStringContainsMiddleName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "Anders",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenUserWithMiddlename_whenSearchStringDoesNotContainsMiddleName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "Jan Hansen",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenUser_whenSearchStringContainsPartOfLastName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "Hans",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenUser_whenSearchStringNotContainsLastName_thenReturnUser() {
        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "Jan Ander",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenTwoUserWithSameLastName_whenSearchStringContainsLastName_thenReturnTwoUsers() {
        User user2 = User.builder()
                .id(125L)
                .firstName("Anne")
                .lastName("Hansen")
                .userType("EMPLOYEESTAFF")
                .build();
        userRepository.save(user2);

        Assignment assignment2 = Assignment.builder()
                .userRef(125L)
                .resourceRef(456L)
                .assignmentRemovedDate(null)
                .build();
        Assignment savedAssignment2 =  assignmentRepository.save(assignment2);

        FlattenedAssignment flattenedAssignment2 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment2.getId())
                .userRef(125L)
                .resourceRef(456L)
                .build();
        flattenedAssignmentRepository.save(flattenedAssignment2);

        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "hansen",
                0,
                10
        );
        assertEquals(2, (resourceAssignmentUserPage.getContent().size()));
    }
    @Test
    void givenTwoUserWithSameLastName_whenSearchStringContainsLastNameAndPartOfFirstName_thenReturnOneUser() {
        User user2 = User.builder()
                .id(125L)
                .firstName("Anne")
                .lastName("Hansen")
                .userType("EMPLOYEESTAFF")
                .build();
        userRepository.save(user2);

        Assignment assignment2 = Assignment.builder()
                .userRef(125L)
                .resourceRef(456L)
                .assignmentRemovedDate(null)
                .build();
        Assignment savedAssignment2 =  assignmentRepository.save(assignment2);

        FlattenedAssignment flattenedAssignment2 = FlattenedAssignment.builder()
                .assignmentId(savedAssignment2.getId())
                .userRef(125L)
                .resourceRef(456L)
                .build();
        flattenedAssignmentRepository.save(flattenedAssignment2);

        Page<ResourceAssignmentUser> resourceAssignmentUserPage = assignmentUserService.findResourceAssignmentUsersForResourceId(
                456L,
                "ALLTYPES",
                allOrgUnitsAsList,
                allOrgUnitsAsList,
                userIds, "anne hansen",
                0,
                10
        );
        assertEquals(1, (resourceAssignmentUserPage.getContent().size()));
        assertEquals("Anne", resourceAssignmentUserPage.getContent().getFirst().getAssigneeFirstName());
    }
}
