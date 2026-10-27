package no.fintlabs.assignment;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.exception.AssignmentException;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.resource.ResourceAssignmentValidator;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private FlattenedAssignmentService flattenedAssignmentService;

    @Mock
    private FlattenedDeviceAssignmentService flattenedDeviceAssignmentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ResourceAssignmentValidator resourceAssignmentValidator;

    @Mock
    private OpaService opaService;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    private AssignmentService assignmentService;

    @Mock
    private ApplicationResourceLocationService applicationResourceLocationService;

    private Assignment userAssignmentWithAssigner;
    private Assignment userAssignmentWithUnknownAssigner;
    private Assignment roleAssignmentWithAssigner;
    private Assignment roleAssignmentWithUnknownAssigner;
    private User userWithDisplayName, userWithOutDisplayName;

    private User userWithEntraId() {
        User user = new User();
        user.setIdentityProviderUserObjectId(UUID.randomUUID());
        return user;
    }

    @BeforeEach
    public void setUp() {
        assignmentService = new AssignmentService(
                roleRepository,
                userRepository,
                assignmentRepository,
                resourceAssignmentValidator,
                flattenedAssignmentService,
                flattenedDeviceAssignmentService,
                applicationResourceLocationService,
                opaService,
                licenseEnforcementService
        );

        userAssignmentWithAssigner = Assignment.builder()
                .assignmentId("2_1_user")
                .resourceRef(2L)
                .userRef(1L)
                .assignerUserName("berntj@viken.no")
                .build();

        userAssignmentWithUnknownAssigner = Assignment.builder()
                .assignmentId("2_3_user")
                .resourceRef(2L)
                .userRef(3L)
                .assignerUserName("ukjent@viken.no")
                .build();

        roleAssignmentWithAssigner = Assignment.builder()
                .assignmentId("2_1_role")
                .resourceRef(2L)
                .roleRef(1L)
                .assignerUserName("berntj@viken.no")
                .build();

        roleAssignmentWithUnknownAssigner = Assignment.builder()
                .assignmentId("2_3_role")
                .resourceRef(2L)
                .roleRef(3L)
                .assignerUserName("ukjent@viken.no")
                .build();

        userWithDisplayName = User.builder()
                .id(3L)
                .userName("berntj@viken.no")
                .firstName("Bernt")
                .lastName("Johnsen")
                .build();

        userWithOutDisplayName = User.builder()
                .id(4L)
                .userName("ukjent@viken.no")
                .build();
    }

    @DisplayName("Test for getAssignerUsernameForUserAssignment - with assigner")
    @Test
    void getAssignerUsernameForUserAssignmentWithAssigner() {

        given(assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(1L, 2L))
                .willReturn(Optional.of(userAssignmentWithAssigner));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForUserAssignment(1L, 2L);
        assertThat(returnedUsername.get()).isEqualTo(userAssignmentWithAssigner.getAssignerUserName());
    }

    @DisplayName("Test for getAssignerDisplaynameForUserAssignment - with assigner displayname")
    @Test
    void getAssignerDisplaynameForUserAssignmenWithDisplayname() {
        given(assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(1L, 2L))
                .willReturn(Optional.of(userAssignmentWithAssigner));
        given(userRepository.getUserByUserName("berntj@viken.no")).willReturn(Optional.of(userWithDisplayName));
        Optional<String> returnedisplayname = assignmentService.getAssignerDisplaynameForUserAssignment(1L, 2L);
        assertThat(returnedisplayname.get()).isEqualTo(userWithDisplayName.getDisplayname());
    }

    @DisplayName("Test for getAssignerUsernameForUserAssignment - without assigner")
    @Test
    void getAssignerUsernameForUserAssignmentWithoutAssigner() {

        Assignment assignment = Assignment.builder()
                .assignmentId("2_2_user")
                .resourceRef(2L)
                .userRef(2L)
                .build();

        given(assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(2L, 2L))
                .willReturn(Optional.of(assignment));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForUserAssignment(2L, 2L);
        assertThat(returnedUsername).isEqualTo(Optional.empty());
    }

    @DisplayName("Test for getAssignerDisplaynameForUserAssignment - without assigner displayname")
    @Test
    void getAssignerDisplaynameForUserAssignmenWithoutDisplayname() {

        given(assignmentRepository.findAssignmentByUserRefAndResourceRefAndAssignmentRemovedDateIsNull(3L, 2L)).willReturn(
                Optional.of(userAssignmentWithUnknownAssigner));
        given(userRepository.getUserByUserName("ukjent@viken.no")).willReturn(Optional.of(userWithOutDisplayName));

        Optional<String> returnedisplayname = assignmentService.getAssignerDisplaynameForUserAssignment(3L, 2L);
        assertThat(returnedisplayname).isEqualTo(Optional.empty());
    }

    @DisplayName("Test for getAssignerUsernameForRoleAssignment - with assigner")
    @Test
    void getAssignerUsernameForRoleAssignmentWithAssigner() {

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(1L, 2L))
                .willReturn(Optional.of(roleAssignmentWithAssigner));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForRoleAssignment(1L, 2L);
        assertThat(returnedUsername.get()).isEqualTo(roleAssignmentWithAssigner.getAssignerUserName());
    }

    @DisplayName("Test for getAssignerDisplaynameForRoleAssignment - with assigner displayname")
    @Test
    void getAssignerDisplaynameForRoleAssignmenWithDisplayname() {
        given(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(1L, 2L))
                .willReturn(Optional.of(roleAssignmentWithAssigner));
        given(userRepository.getUserByUserName("berntj@viken.no")).willReturn(Optional.of(userWithDisplayName));
        Optional<String> returnedisplayname = assignmentService.getAssignerDisplaynameForRoleAssignment(1L, 2L);
        assertThat(returnedisplayname.get()).isEqualTo(userWithDisplayName.getDisplayname());
    }

    @DisplayName("Test for getAssignerUsernameForRoleAssignment - without assigner")
    @Test
    void getAssignerUsernameForRoleAssignmentWithoutAssigner() {

        Assignment assignment = Assignment.builder()
                .assignmentId("2_2_role")
                .resourceRef(2L)
                .roleRef(2L)
                .build();

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(2L, 2L))
                .willReturn(Optional.of(assignment));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForRoleAssignment(2L, 2L);
        assertThat(returnedUsername).isEqualTo(Optional.empty());
    }

    @DisplayName("Test for getAssignerDisplaynameForRoleAssignment - without assigner displayname")
    @Test
    void getAssignerDisplaynameForRoleAssignmenWithoutDisplayname() {

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRefAndAssignmentRemovedDateIsNull(3L, 2L)).willReturn(
                Optional.of(roleAssignmentWithUnknownAssigner));
        given(userRepository.getUserByUserName("ukjent@viken.no")).willReturn(Optional.of(userWithOutDisplayName));

        Optional<String> returnedisplayname = assignmentService.getAssignerDisplaynameForRoleAssignment(3L, 2L);
        assertThat(returnedisplayname).isEqualTo(Optional.empty());
    }

    //    @Test
    //    void getAssignerUsernameForRoleAssignment() {
    //    }
    //
    //    @Test
    //    void getAssignerDisplaynameForRoleAssignment() {
    //    }

    @Test
    void shouldCreateNewAssignment_ValidReferences_CreatesNewAssignment() {
        Assignment assignment = Assignment.builder()
                .userRef(1L)
                .resourceRef(1L)
                .organizationUnitId("orgid1")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(userWithEntraId()));
        given(resourceAssignmentValidator.getActiveResourceOrThrow(1L)).willReturn(Resource.builder().id(1L).resourceName("Resource 1").status("ACTIVE").build());
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);
        given(licenseEnforcementService.validateAssignmentCapacity(any(Assignment.class))).willReturn(true);

        NearestResourceLocationDto nearestResourceLocationDto = new NearestResourceLocationDto("orgid1", "OrgUnit no 1");
        given(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "orgid1")).willReturn(Optional.of(nearestResourceLocationDto));

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid1", 1L, null);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(licenseEnforcementService,times(1)).validateAssignmentCapacity(isA(Assignment.class));
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService).createFlattenedAssignmentsSync(assignment);
    }

    @Test
    void shouldCreateFlattenedAssignmentsBeforeTransactionCommit() {
        Assignment assignment = Assignment.builder()
                .id(123L)
                .userRef(1L)
                .resourceRef(1L)
                .organizationUnitId("orgid1")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(userWithEntraId()));
        given(resourceAssignmentValidator.getActiveResourceOrThrow(1L)).willReturn(Resource.builder().id(1L).resourceName("Resource 1").status("ACTIVE").build());
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);
        given(licenseEnforcementService.validateAssignmentCapacity(any(Assignment.class))).willReturn(true);
        given(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "orgid1")).willReturn(Optional.empty());

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid1", 1L, null);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(flattenedAssignmentService).createFlattenedAssignmentsSync(assignment);
        verify(flattenedAssignmentService, never()).createFlattenedAssignments(any());
    }

    @Test
    void shouldCreateNewAssignment_InvalidUserReference_ThrowsResourceNotFoundException() {
        given(userRepository.findById(999L)).willThrow(new ResourceNotFoundException("User not found: 999"));

        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 999L, 1L));
    }

    @Test
    void shouldCreateNewAssignment_InvalidRoleReference_ThrowsRoleNotFoundException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithEntraId()));
        given(roleRepository.findById(999L)).willThrow(new RoleNotFoundException("999"));

        assertThrows(RoleNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 1L, 999L));
    }

    @Test
    void shouldCreateNewAssignment_InvalidResourceReference_ThrowsResourceNotFoundException() {
        given(resourceAssignmentValidator.getActiveResourceOrThrow(999L)).willThrow(new ResourceNotFoundException("Resource not found for ref: 999"));

        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createNewAssignment(999L, "orgid", 1L, 1L));
    }

    @Test
    void shouldCreateNewAssignment_InactiveResource_ThrowsAssignmentException() {
        given(resourceAssignmentValidator.getActiveResourceOrThrow(1L)).willThrow(new AssignmentException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Resource 1 is not active"));

        assertThrows(AssignmentException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 1L, null));

        verify(assignmentRepository, times(0)).saveAndFlush(any());
        verify(flattenedAssignmentService, times(0)).createFlattenedAssignments(any());
    }

    @Test
    void shouldDeleteAssignment_ValidId_DeletesAssignment() {
        Long validId = 1L;
        String userName = "testUser";
        User user = new User();
        user.setUserName(userName);
        Assignment assignment = new Assignment();

        given(opaService.getUserNameAuthenticatedUser()).willReturn(userName);
        given(userRepository.getUserByUserName(userName)).willReturn(Optional.of(user));
        given(assignmentRepository.getReferenceById(validId)).willReturn(assignment);
        given(assignmentRepository.saveAndFlush(any())).willAnswer(invocation -> {
            Assignment savedAssignment = invocation.getArgument(0);
            assertThat(savedAssignment.getAssignmentRemovedDate()).isNotNull();
            assertThat(savedAssignment.getAssignerRemoveRef()).isEqualTo(user.getId());
            return savedAssignment;
        });

        Assignment returnedAssignment = assignmentService.deleteAssignment(validId);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(assignment);
        verify(flattenedAssignmentService, times(1)).deleteFlattenedAssignments(any(), anyString());
    }

    @Test
    void shouldDeleteAssignment_InvalidUser_shouldNotSetAssignerRemoveRef() {
        Long validId = 1L;
        String invalidUserName = "invalidUser";

        given(opaService.getUserNameAuthenticatedUser()).willReturn(invalidUserName);
        given(userRepository.getUserByUserName(invalidUserName)).willReturn(Optional.empty());

        Assignment assignment = new Assignment();

        given(assignmentRepository.getReferenceById(validId)).willReturn(assignment);

        assignmentService.deleteAssignment(validId);

        assertThat(assignment.getAssignmentRemovedDate()).isNotNull();
        assertThat(assignment.getAssignerRemoveRef()).isNull();
    }

    @Test
    public void shouldDeactivateAssignmentsIfUserInactive() {
        User user = new User();
        user.setId(1L);
        user.setStatus("disabled");

        Assignment assignment = new Assignment();
        List<Assignment> assignments = List.of(assignment);
        when(assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(user.getId())).thenReturn(assignments);

        assignmentService.deactivateAssignmentsByUserId(user.getId());

        verify(assignmentRepository).saveAndFlush(assignment);
        verify(flattenedAssignmentService).deleteFlattenedAssignments(assignment, "User is no longer active");

        assertNotNull(assignment.getAssignmentRemovedDate());
    }

    @Test
    void getActiveAssignmentsByResource_shouldReturnOnlyUserAndRoleAssignments() {
        List<Assignment> expected = List.of(new Assignment(), new Assignment());

        when(assignmentRepository.findActiveUserAssignmentsByResourceRef(1L)).thenReturn(expected);

        List<Assignment> actual = assignmentService.getActiveUserAssignmentsByResource(1L);

        assertThat(actual).isSameAs(expected);
        verify(assignmentRepository).findActiveUserAssignmentsByResourceRef(1L);
    }

    @Test
    void deactivateAssignmentsByResourceId_shouldRecalculateAssignedResourcesAfterDeactivation() {
        Assignment roleAssignment = Assignment.builder()
                .id(10L)
                .roleRef(5L)
                .resourceRef(1L)
                .build();

        when(assignmentRepository.findActiveUserAssignmentsByResourceRef(1L)).thenReturn(List.of(roleAssignment));

        assignmentService.deactivateAssignmentsByResourceId(1L);

        verify(assignmentRepository).saveAndFlush(roleAssignment);
        verify(flattenedAssignmentService).deleteFlattenedAssignments(roleAssignment, "Resource is no longer active");
        verify(licenseEnforcementService).recalculateAssignedResources(roleAssignment);
    }

    @Test
    void reassignLinkedAssignments_updatesActiveAssignmentsAndTheirUserProjections() {
        Assignment assignment = Assignment.builder()
                .id(10L)
                .resourceRef(1L)
                .userRef(2L)
                .organizationUnitId("user-org-unit")
                .applicationResourceLocationOrgUnitId("removed-org-unit")
                .build();
        NearestResourceLocationDto nearestLocation = new NearestResourceLocationDto("nearest-org-unit", "Nearest");

        given(assignmentRepository.findAssignmentsByResourceRefAndApplicationResourceLocationOrgUnitIdAndAssignmentRemovedDateIsNull(1L, "removed-org-unit"))
                .willReturn(List.of(assignment));
        given(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "user-org-unit"))
                .willReturn(Optional.of(nearestLocation));

        assignmentService.reassignLinkedAssignments(1L, "removed-org-unit");

        assertThat(assignment.getApplicationResourceLocationOrgUnitId()).isEqualTo("nearest-org-unit");
        verify(assignmentRepository).saveAll(List.of(assignment));
        verify(assignmentRepository).flush();
        verify(flattenedAssignmentService).syncFlattenedAssignmentsSynchronously(assignment, false);
        verify(flattenedDeviceAssignmentService, never()).updateApplicationResourceLocation(any());
    }

    @Test
    void reassignLinkedAssignments_updatesDeviceProjections() {
        Assignment assignment = Assignment.builder()
                .id(10L)
                .resourceRef(1L)
                .deviceGroupRef(2L)
                .organizationUnitId("user-org-unit")
                .applicationResourceLocationOrgUnitId("removed-org-unit")
                .build();

        given(assignmentRepository.findAssignmentsByResourceRefAndApplicationResourceLocationOrgUnitIdAndAssignmentRemovedDateIsNull(1L, "removed-org-unit"))
                .willReturn(List.of(assignment));
        given(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "user-org-unit"))
                .willReturn(Optional.empty());

        assignmentService.reassignLinkedAssignments(1L, "removed-org-unit");

        verify(flattenedDeviceAssignmentService).updateApplicationResourceLocation(assignment);
        verify(flattenedAssignmentService, never()).syncFlattenedAssignmentsSynchronously(any(), anyBoolean());
    }

    // Copilot tests for createNewAssignment
    @DisplayName("Create new assignment - valid user reference")
    @Test
    void shouldCreateNewAssignment_ValidUserReference_CreatesNewAssignment() {
        Assignment assignment = Assignment.builder()
                .userRef(1L)
                .resourceRef(1L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(userWithEntraId()));
        given(resourceAssignmentValidator.getActiveResourceOrThrow(1L)).willReturn(Resource.builder().id(1L).resourceName("Resource 1").status("ACTIVE").build());
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);
        given(licenseEnforcementService.validateAssignmentCapacity(any(Assignment.class))).willReturn(true);

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid", 1L, null);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService).createFlattenedAssignmentsSync(assignment);
    }

    @DisplayName("Create new assignment - valid role reference")
    @Test
    void shouldCreateNewAssignment_ValidRoleReference_CreatesNewAssignment() {
        Assignment assignment = Assignment.builder()
                .roleRef(1L)
                .resourceRef(1L)
                .build();

        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
        given(resourceAssignmentValidator.getActiveResourceOrThrow(1L)).willReturn(Resource.builder().id(1L).resourceName("Resource 1").status("ACTIVE").build());
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);
        given(licenseEnforcementService.validateAssignmentCapacity(any(Assignment.class))).willReturn(true);

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid", null, 1L);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService).createFlattenedAssignmentsSync(assignment);
    }

//    @DisplayName("Create new assignment - invalid user reference")
//    @Test
//    void shouldCreateNewAssignment_InvalidUserReference_ThrowsUserNotFoundException() {
//        given(userRepository.findById(999L)).willThrow(new UserNotFoundException("999"));
//
//        assertThrows(UserNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 999L, 1L));
//    }
//
//    @DisplayName("Create new assignment - invalid role reference")
//    @Test
//    void shouldCreateNewAssignment_InvalidRoleReference_ThrowsRoleNotFoundException() {
//        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
//        given(roleRepository.findById(999L)).willThrow(new RoleNotFoundException("999"));
//
//        assertThrows(RoleNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 1L, 999L));
//    }
//
//    @DisplayName("Create new assignment - invalid resource reference")
//    @Test
//    void shouldCreateNewAssignment_InvalidResourceReference_ThrowsResourceNotFoundException() {
//        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
//        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
//        given(resourceRepository.getReferenceById(999L)).willThrow(new ResourceNotFoundException("999"));
//
//        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createNewAssignment(999L, "orgid", 1L, 1L));
//    }
//
//    @DisplayName("Delete assignment - valid id")
//    @Test
//    void shouldDeleteAssignment_ValidId_DeletesAssignment() {
//        Long validId = 1L;
//        String userName = "testUser";
//        User user = new User();
//        user.setUserName(userName);
//        Assignment assignment = new Assignment();
//
//        given(opaService.getUserNameAuthenticatedUser()).willReturn(userName);
//        given(userRepository.getUserByUserName(userName)).willReturn(Optional.of(user));
//        given(assignmentRepository.getReferenceById(validId)).willReturn(assignment);
//        given(assignmentRepository.saveAndFlush(any())).willAnswer(invocation -> {
//            Assignment savedAssignment = invocation.getArgument(0);
//            assertThat(savedAssignment.getAssignmentRemovedDate()).isNotNull();
//            assertThat(savedAssignment.getAssignerRemoveRef()).isEqualTo(user.getId());
//            return savedAssignment;
//        });
//
//        Assignment returnedAssignment = assignmentService.deleteAssignment(validId);
//
//        assertThat(returnedAssignment).isEqualTo(assignment);
//        verify(assignmentRepository, times(1)).saveAndFlush(assignment);
//        verify(flattenedAssignmentService, times(1)).deleteFlattenedAssignments(any());
//    }
//
//    @DisplayName("Delete assignment - invalid user")
//    @Test
//    void shouldDeleteAssignment_InvalidUser_shouldNotSetAssignerRemoveRef() {
//        Long validId = 1L;
//        String invalidUserName = "invalidUser";
//
//        given(opaService.getUserNameAuthenticatedUser()).willReturn(invalidUserName);
//        given(userRepository.getUserByUserName(invalidUserName)).willReturn(Optional.empty());
//
//        Assignment assignment = new Assignment();
//
//        given(assignmentRepository.getReferenceById(validId)).willReturn(assignment);
//
//        assignmentService.deleteAssignment(validId);
//
//        assertThat(assignment.getAssignmentRemovedDate()).isNotNull();
//        assertThat(assignment.getAssignerRemoveRef()).isNull();
//    }
//
//    @DisplayName("Deactivate assignments if user inactive")
//    @Test
//    public void shouldDeactivateAssignmentsIfUserInactive() {
//        User user = new User();
//        user.setId(1L);
//        user.setStatus("disabled");
//
//        Assignment assignment = new Assignment();
//        List<Assignment> assignments = List.of(assignment);
//        when(assignmentRepository.findAssignmentsByUserRefAndAssignmentRemovedDateIsNull(user.getId())).thenReturn(assignments);
//
//        assignmentService.deactivateAssignmentsByUser(user);
//
//        verify(assignmentRepository).saveAndFlush(assignment);
//        verify(flattenedAssignmentService).deleteFlattenedAssignments(assignment);
//
//        assertNotNull(assignment.getAssignmentRemovedDate());
//    }
}
