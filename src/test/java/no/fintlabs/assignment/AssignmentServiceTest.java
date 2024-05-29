package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
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

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private AssignmentService assignmentService;

    private Assignment userAssignmentWithAssigner, userAssignmentWithUnknownAssigner, roleAssignmentWithAssigner,
            roleAssignmentWithUnknownAssigner;
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
                .roleRef(1L)
                .resourceRef(1L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
        given(resourceRepository.findById(1L)).willReturn(Optional.of(new Resource()));
        given(assignmentRepository.saveAndFlush(any())).willReturn(assignment);

        Assignment returnedAssignment = assignmentService.createNewAssignment(assignment);

        assertThat(returnedAssignment).isEqualTo(assignment);
        verify(assignmentRepository, times(1)).saveAndFlush(any());
        verify(flattenedAssignmentService, times(1)).createFlattenedAssignments(any());
    }

    @Test
    void shouldCreateNewAssignment_InvalidUserReference_ThrowsUserNotFoundException() {
        Assignment assignment = Assignment.builder()
                .userRef(999L)
                .roleRef(1L)
                .resourceRef(1L)
                .build();

        given(userRepository.findById(999L)).willThrow(new UserNotFoundException("999"));

        assertThrows(UserNotFoundException.class, () -> assignmentService.createNewAssignment(assignment));
    }

    @Test
    void shouldCreateNewAssignment_InvalidRoleReference_ThrowsRoleNotFoundException() {
        Assignment assignment = Assignment.builder()
                .userRef(1L)
                .roleRef(999L)
                .resourceRef(1L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(roleRepository.findById(999L)).willThrow(new RoleNotFoundException("999"));

        assertThrows(RoleNotFoundException.class, () -> assignmentService.createNewAssignment(assignment));
    }

    @Test
    void shouldCreateNewAssignment_InvalidResourceReference_ThrowsResourceNotFoundException() {
        Assignment assignment = Assignment.builder()
                .userRef(1L)
                .roleRef(1L)
                .resourceRef(999L)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(new User()));
        given(roleRepository.findById(1L)).willReturn(Optional.of(new Role()));
        given(resourceRepository.findById(999L)).willThrow(new ResourceNotFoundException("999"));

        assertThrows(ResourceNotFoundException.class, () -> assignmentService.createNewAssignment(assignment));
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
        verify(flattenedAssignmentService, times(1)).updateFlattenedAssignment(any());
    }

    @Test
    void shouldDeleteAssignment_InvalidUser_ThrowsUserNotFoundException() {
        Long validId = 1L;
        String invalidUserName = "invalidUser";

        given(opaService.getUserNameAuthenticatedUser()).willReturn(invalidUserName);
        given(userRepository.getUserByUserName(invalidUserName)).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> assignmentService.deleteAssignment(validId));
    }

}
