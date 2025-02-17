package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@ExtendWith(MockitoExtension.class)
class LicenseEnforcementServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ApplicationResourceLocationRepository applicationResourceLocationRepository;

    @Mock
    private RoleRepository roleRepository;

    private AssignmentService assignmentService;

    @InjectMocks
    private LicenseEnforcementService licenseEnforcementService;

    private Resource resourceHardstop, resourceFree;
    private ApplicationResourceLocation applicationResourceLocationHardstop, applicationResourceLocationFreeall;
    private Assignment assignmentToUser, assignmentToRole;
    private User user;
    private Role role;


    @BeforeEach
    void setUp() {
        resourceHardstop = Resource.builder()
                .id(1L)
                .resourceId("app1")
                .resourceType("allTypes")
                .numberOfResourcesAssigned(100L)
                .resourceLimit(200L)
                .licenseEnforcement(Handhevingstype.HARDSTOP.name())
                .build();

        resourceFree = Resource.builder()
                .id(2L)
                .resourceId("app2")
                .resourceType("allTypes")
                .numberOfResourcesAssigned(1000L)
                .licenseEnforcement(Handhevingstype.FREEALL.name())
                .build();


        applicationResourceLocationHardstop = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(1L)
                .resourceId("app1")
                .orgUnitId("org1")
                .orgUnitName("OrgUnit no 1")
                .resourceLimit(50L)
                .numberOfResourcesAssigned(30L)
                .build();

        applicationResourceLocationFreeall = ApplicationResourceLocation
                .builder()
                .id(1L)
                .applicationResourceId(2L)
                .resourceId("app2")
                .orgUnitId("org2")
                .orgUnitName("OrgUnit no 2")
                .numberOfResourcesAssigned(1000L)
                .build();

        assignmentToUser = Assignment.builder()
                .id(111L)
                .assignerRef(222L)
                .userRef(333L)
                .azureAdUserId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .resourceRef(1L)
                .organizationUnitId("org1")
                .resourceConsumerOrgUnitId("org1")
                .assignmentDate(new Date())
                .validTo(new Date())
                .build();

        assignmentToRole = Assignment.builder()
                .id(111L)
                .assignerRef(222L)
                .resourceRef(1L)
                .azureAdGroupId(UUID.fromString("456e4567-e89b-12d3-a456-426614174000"))
                .roleRef(555L)
                .organizationUnitId("org1")
                .resourceConsumerOrgUnitId("org1")
                .assignmentDate(new Date())
                .validTo(new Date())
                .build();

        user = User.builder()
                .id(333L)
                .userName("titten@tei.no")
                .firstName("Titten")
                .lastName("Tei")
                .organisationUnitId("org1")
                .build();

        role = Role.builder()
                .id(1L)
                .noOfMembers(10L)
                .build();

    }

    @Test
    public void incrementAssignLicensesHardstopForUser() {

        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToUser.getResourceConsumerOrgUnitId()))
                .willReturn(Optional.ofNullable(applicationResourceLocationHardstop));

        boolean licenseUpdated = licenseEnforcementService.updateAssignedLicenses(assignmentToUser, resourceHardstop.getId());

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(101);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(31L);
        assertThat(licenseUpdated).isTrue();
    }

    @Test
    public void incrementAssignLicensesHardstopForRole() {
        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(roleRepository.findById(555L)).willReturn(Optional.ofNullable(role));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToRole.getResourceConsumerOrgUnitId()))
                .willReturn(Optional.ofNullable(applicationResourceLocationHardstop));

        boolean licenseUpdated = licenseEnforcementService.updateAssignedLicenses(assignmentToRole, resourceHardstop.getId());

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(110);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(40L);
        assertThat(licenseUpdated).isTrue();
    }

    @Test
    public void incrementAssignLicensesFreeAllForUser() {

        given(resourceRepository.findById(2L)).willReturn(Optional.ofNullable(resourceFree));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceFree.getId(), assignmentToUser.getResourceConsumerOrgUnitId()))
                .willReturn(Optional.ofNullable(applicationResourceLocationFreeall));

        boolean licenseUpdated = licenseEnforcementService.updateAssignedLicenses(assignmentToUser, resourceFree.getId());

        assertThat(resourceFree.getNumberOfResourcesAssigned()).isEqualTo(1001);
        assertThat(applicationResourceLocationFreeall.getNumberOfResourcesAssigned()).isEqualTo(1001L);
        assertThat(licenseUpdated).isTrue();
    }

    @Test
    public void incrementAssignLicensesFreeAllForRole() {
        given(resourceRepository.findById(2L)).willReturn(Optional.ofNullable(resourceFree));
        given(roleRepository.findById(555L)).willReturn(Optional.ofNullable(role));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceFree.getId(), assignmentToRole.getResourceConsumerOrgUnitId()))
                .willReturn(Optional.ofNullable(applicationResourceLocationFreeall));

        boolean licenseUpdated = licenseEnforcementService.updateAssignedLicenses(assignmentToRole, resourceFree.getId());

        assertThat(resourceFree.getNumberOfResourcesAssigned()).isEqualTo(1010);
        assertThat(applicationResourceLocationFreeall.getNumberOfResourcesAssigned()).isEqualTo(1010L);
        assertThat(licenseUpdated).isTrue();
    }
}