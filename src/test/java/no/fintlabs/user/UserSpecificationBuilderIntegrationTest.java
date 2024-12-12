package no.fintlabs.user;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.opa.model.OrgUnitType;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.BDDAssumptions.given;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class UserSpecificationBuilderIntegrationTest extends DatabaseIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;

    User user = User.builder()
            .id(1L)
            .firstName("Jan Anders")
            .lastName("Hansen")
            .userType("EMPLOYEESTAFF")
            .build();

    List<String> allOrgUnitsAsList = List.of(OrgUnitType.ALLORGUNITS.name());

    Assignment assignment = Assignment.builder()
            .id(1L)
            .userRef(1L)
            .resourceRef(1L)
            .assignmentRemovedDate(null)
            .build();

    Resource resource = Resource.builder()
            .id(1L)
            .resourceName("Adobek12")
            .build();

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        resourceRepository.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.save(user);
        resourceRepository.save(resource);
        assignmentRepository.save(assignment);
    }
    @Test
    void givenUserWithMiddlename_whenSearchStringContainsMiddleName_thenReturnUser() {
        UserSpecificationBuilder userSpecificationBuilder = new UserSpecificationBuilder(1L, "ALLTYPES",allOrgUnitsAsList , allOrgUnitsAsList, "Anders");
        Specification<User> spec = userSpecificationBuilder.assignmentSearch();
        List<User> users = userRepository.findAll(spec);

        assertEquals(1, (users.size()));
    }
    @Test
    void givenUser_whenSearchStringContainsPartOfLastName_thenReturnUser() {
        UserSpecificationBuilder userSpecificationBuilder = new UserSpecificationBuilder(1L, "ALLTYPES",allOrgUnitsAsList , allOrgUnitsAsList, "Hans");
        Specification<User> spec = userSpecificationBuilder.assignmentSearch();
        List<User> users = userRepository.findAll(spec);

        assertEquals(1, (users.size()));
    }
}