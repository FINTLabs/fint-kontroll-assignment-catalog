package no.fintlabs.enforcement;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocation;
import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationRepository;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.kodeverk.Handhevingstype;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceAvailabilityPublishingComponent;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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

    @Mock
    private ResourceAvailabilityPublishingComponent resourceAvailabilityPublishingComponent;

    @InjectMocks
    private LicenseEnforcementService licenseEnforcementService;

    private Resource resourceHardstop, resourceFree;
    private ApplicationResourceLocation applicationResourceLocationHardstop, applicationResourceLocationFreeall;
    private Assignment assignmentToUserHardstop, assignmentToRoleHardstop, assignmentToRoleFree, assignmentToUserFree;
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
                .id(2L)
                .applicationResourceId(2L)
                .resourceId("app2")
                .orgUnitId("org2")
                .orgUnitName("OrgUnit no 2")
                .numberOfResourcesAssigned(1000L)
                .build();

        assignmentToUserHardstop = Assignment.builder()
                .id(111L)
                .assignerRef(222L)
                .userRef(333L)
                .resourceRef(1L)
                .organizationUnitId("org1")
                .applicationResourceLocationOrgUnitId("org1")
                .azureAdUserId(UUID.randomUUID())
                .build();

        assignmentToRoleHardstop = Assignment.builder()
                .id(222L)
                .assignerRef(222L)
                .resourceRef(1L)
                .roleRef(555L)
                .organizationUnitId("org1")
                .applicationResourceLocationOrgUnitId("org1")
                .azureAdGroupId(UUID.randomUUID())
                .build();

        assignmentToRoleFree = Assignment.builder()
                .id(333L)
                .assignerRef(222L)
                .resourceRef(2L)
                .roleRef(555L)
                .organizationUnitId("org1")
                .applicationResourceLocationOrgUnitId("org1")
                .azureAdGroupId(UUID.randomUUID())
                .build();

        assignmentToUserFree = Assignment.builder()
                .id(444L)
                .assignerRef(222L)
                .userRef(333L)
                .resourceRef(2L)
                .organizationUnitId("org1")
                .applicationResourceLocationOrgUnitId("org1")
                .azureAdUserId(UUID.randomUUID())
                .build();

        role = Role.builder()
                .id(1L)
                .noOfMembers(10L)
                .build();
    }

    @DisplayName("Test for user assignment. Resource has hardstop")
    @Test
    public void incrementAssignLicensesHardstopForUser() {

        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToUserHardstop.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationHardstop)));

        boolean licenseUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToUserHardstop);

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(101);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(31L);
        assertThat(licenseUpdated).isTrue();
    }

    @DisplayName("Test for role assignment. Resource has hardstop")
    @Test
    public void incrementAssignLicensesHardstopForRole() {
        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(roleRepository.findById(555L)).willReturn(Optional.ofNullable(role));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToRoleHardstop.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationHardstop)));

        boolean licenseUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToRoleHardstop);

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(110);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(40L);
        assertThat(licenseUpdated).isTrue();
    }

    @DisplayName("Test for user assignment. Resource has freeforall")
    @Test
    public void incrementAssignLicensesFreeAllForUser() {

        given(resourceRepository.findById(2L)).willReturn(Optional.ofNullable(resourceFree));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceFree.getId(), assignmentToRoleFree.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationFreeall)));

        boolean licenseUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToUserFree);

        assertThat(resourceFree.getNumberOfResourcesAssigned()).isEqualTo(1001);
        assertThat(applicationResourceLocationFreeall.getNumberOfResourcesAssigned()).isEqualTo(1001L);
        assertThat(licenseUpdated).isTrue();
    }

    @DisplayName("Test for role assignment. Resource has freeforall")
    @Test
    public void incrementAssignLicensesFreeAllForRole() {
        given(resourceRepository.findById(2L)).willReturn(Optional.ofNullable(resourceFree));
        given(roleRepository.findById(555L)).willReturn(Optional.ofNullable(role));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(assignmentToRoleFree.getResourceRef(), assignmentToRoleFree.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationFreeall)));

        boolean licenseUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToRoleFree);

        assertThat(resourceFree.getNumberOfResourcesAssigned()).isEqualTo(1010);
        assertThat(applicationResourceLocationFreeall.getNumberOfResourcesAssigned()).isEqualTo(1010L);
        assertThat(licenseUpdated).isTrue();
    }

    @DisplayName("Test for user assignment. Resource has hardstop and assigment exceeded resourceLimit for resource")
    @Test
    public void doNotAssignLicensesHardstopForUserExeededResourceLimit() {
        resourceHardstop.setResourceLimit(100L);
        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToUserHardstop.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationHardstop)));
        ReflectionTestUtils.setField(licenseEnforcementService, "hardstopEnabled", true);

        boolean lisensUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToUserHardstop);

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(100L);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(30L);
        assertThat(lisensUpdated).isFalse();
    }

    @DisplayName("Test for user assignment. Resource has hardstop and assigment exceeded resourceLimit for applicationResourceLocation")
    @Test
    public void doNotAssignLicensesHardstopForUserExeededConsumerResourceLimit() {
        applicationResourceLocationHardstop.setResourceLimit(30L);
        given(resourceRepository.findById(1L)).willReturn(Optional.ofNullable(resourceHardstop));
        given(applicationResourceLocationRepository
                .findByApplicationResourceIdAndOrgUnitId(resourceHardstop.getId(), assignmentToUserHardstop.getApplicationResourceLocationOrgUnitId()))
                .willReturn(Optional.of(List.of(applicationResourceLocationHardstop)));
        ReflectionTestUtils.setField(licenseEnforcementService, "hardstopEnabled", true);

        boolean licenseUpdated = licenseEnforcementService.incrementAssignedLicensesWhenNewAssignment(assignmentToUserHardstop);

        assertThat(resourceHardstop.getNumberOfResourcesAssigned()).isEqualTo(100L);
        assertThat(applicationResourceLocationHardstop.getNumberOfResourcesAssigned()).isEqualTo(30L);
        assertThat(licenseUpdated).isFalse();
    }
}