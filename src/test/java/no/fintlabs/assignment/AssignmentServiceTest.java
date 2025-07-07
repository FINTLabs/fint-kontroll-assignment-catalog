package no.fintlabs.assignment;

import no.fintlabs.applicationresourcelocation.ApplicationResourceLocationService;
import no.fintlabs.applicationresourcelocation.NearestResourceLocationDto;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceNotFoundException;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.role.Role;
import no.fintlabs.role.RoleNotFoundException;
import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserNotFoundException;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
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
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private OpaService opaService;

    @Mock
    private LicenseEnforcementService licenseEnforcementService;

    @InjectMocks
    private AssignmentService assignmentService;

    @Mock
    private ApplicationResourceLocationService applicationResourceLocationService;

    private Assignment userAssignmentWithAssigner, userAssignmentWithUnknownAssigner, roleAssignmentWithAssigner;
    private User userWithDisplayName, userWithOutDisplayName;

    @BeforeEach
    public void setUp() {
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

        userAssignmentWithUnknownAssigner = Assignment.builder()
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
                Optional.of(userAssignmentWithUnknownAssigner));
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

        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(resourceRepository.findById(1L)).willReturn(Optional.of(new Resource()));
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);

        NearestResourceLocationDto nearestResourceLocationDto = new NearestResourceLocationDto("orgid1", "OrgUnit no 1");
        given(applicationResourceLocationService.getNearestApplicationResourceLocationForOrgUnit(1L, "orgid1")).willReturn(Optional.of(nearestResourceLocationDto));

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid1", 1L, null);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(licenseEnforcementService,times(1)).incrementAssignedLicensesWhenNewAssignment(isA(Assignment.class));
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService, times(1)).createFlattenedAssignments(any());
    }

    @Test
    void shouldCreateNewAssignment_InvalidUserReference_ThrowsUserNotFoundException() {
        given(userRepository.findById(999L)).willThrow(new UserNotFoundException("999"));

        assertThrows(UserNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 999L, 1L));
    }

    @Test
    void shouldCreateNewAssignment_InvalidRoleReference_ThrowsRoleNotFoundException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(roleRepository.findById(999L)).willThrow(new RoleNotFoundException("999"));

        assertThrows(RoleNotFoundException.class, () -> assignmentService.createNewAssignment(1L, "orgid", 1L, 999L));
    }

    @Test
    void shouldCreateNewAssignment_InvalidResourceReference_ThrowsResourceNotFoundException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
        given(resourceRepository.findById(999L)).willThrow(new ResourceNotFoundException("999"));

        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createNewAssignment(999L, "orgid", 1L, 1L));
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
        verify(flattenedAssignmentService, times(1)).deleteFlattenedAssignments(any());
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

        assignmentService.deactivateAssignmentsByUser(user);

        verify(assignmentRepository).saveAndFlush(assignment);
        verify(flattenedAssignmentService).deleteFlattenedAssignments(assignment);

        assertNotNull(assignment.getAssignmentRemovedDate());
    }

    // Copilot tests for createNewAssignment
    @DisplayName("Create new assignment - valid user reference")
    @Test
    void shouldCreateNewAssignment_ValidUserReference_CreatesNewAssignment() {
        Assignment assignment = Assignment.builder()
                .userRef(1L)
                .resourceRef(1L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(resourceRepository.findById(1L)).willReturn(Optional.of(new Resource()));
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid", 1L, null);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService, times(1)).createFlattenedAssignments(any());
    }

    @DisplayName("Create new assignment - valid role reference")
    @Test
    void shouldCreateNewAssignment_ValidRoleReference_CreatesNewAssignment() {
        Assignment assignment = Assignment.builder()
                .roleRef(1L)
                .resourceRef(1L)
                .build();

        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
        given(resourceRepository.findById(1L)).willReturn(Optional.of(new Resource()));
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);

        Assignment returnedAssignment = assignmentService.createNewAssignment(1L, "orgid", null, 1L);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService, times(1)).createFlattenedAssignments(any());
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
//        given(resourceRepository.findById(999L)).willThrow(new ResourceNotFoundException("999"));
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
