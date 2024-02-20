package no.fintlabs.assignment;

import no.fintlabs.role.RoleRepository;
import no.fintlabs.user.User;
import no.fintlabs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTests {
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AssignmentService assignmentService;
    private Assignment userAssignmentWithAssigner, userAssignmentWithUnknownAssigner, roleAssignmentWithAssigner, roleAssignmentWithUnknownAssigner;
    private User userWithDisplayName,userWithOutDisplayName;

    @BeforeEach
    public void SetUp() {
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

        given(assignmentRepository.findAssignmentByUserRefAndResourceRef(1L,2L))
                .willReturn(Optional.of(userAssignmentWithAssigner));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForUserAssignment(1L,2L);
        assertThat(returnedUsername.get()).isEqualTo(userAssignmentWithAssigner.getAssignerUserName());
    }
    @DisplayName("Test for getAssignerDisplaynameForUserAssignment - with assigner displayname")
    @Test
    void getAssignerDisplaynameForUserAssignmenWithDisplayname() {
        given(assignmentRepository.findAssignmentByUserRefAndResourceRef(1L,2L))
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

        given(assignmentRepository.findAssignmentByUserRefAndResourceRef(2L,2L))
                .willReturn(Optional.of(assignment));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForUserAssignment(2L,2L);
        assertThat(returnedUsername).isEqualTo(Optional.empty());
    }
    @DisplayName("Test for getAssignerDisplaynameForUserAssignment - without assigner displayname")
    @Test
    void getAssignerDisplaynameForUserAssignmenWithoutDisplayname() {

        given(assignmentRepository.findAssignmentByUserRefAndResourceRef(3L,2L)).willReturn(Optional.of(userAssignmentWithUnknownAssigner));
        given(userRepository.getUserByUserName("ukjent@viken.no")).willReturn(Optional.of(userWithOutDisplayName));

        Optional<String> returnedisplayname = assignmentService.getAssignerDisplaynameForUserAssignment(3L, 2L);
        assertThat(returnedisplayname).isEqualTo(Optional.empty());
    }

    @DisplayName("Test for getAssignerUsernameForRoleAssignment - with assigner")
    @Test
    void getAssignerUsernameForRoleAssignmentWithAssigner() {

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRef(1L,2L))
                .willReturn(Optional.of(roleAssignmentWithAssigner));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForRoleAssignment(1L,2L);
        assertThat(returnedUsername.get()).isEqualTo(roleAssignmentWithAssigner.getAssignerUserName());
    }
    @DisplayName("Test for getAssignerDisplaynameForRoleAssignment - with assigner displayname")
    @Test
    void getAssignerDisplaynameForRoleAssignmenWithDisplayname() {
        given(assignmentRepository.findAssignmentByRoleRefAndResourceRef(1L,2L))
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

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRef(2L,2L))
                .willReturn(Optional.of(assignment));

        Optional<String> returnedUsername = assignmentService.getAssignerUsernameForRoleAssignment(2L,2L);
        assertThat(returnedUsername).isEqualTo(Optional.empty());
    }
    @DisplayName("Test for getAssignerDisplaynameForRoleAssignment - without assigner displayname")
    @Test
    void getAssignerDisplaynameForRoleAssignmenWithoutDisplayname() {

        given(assignmentRepository.findAssignmentByRoleRefAndResourceRef(3L,2L)).willReturn(Optional.of(userAssignmentWithUnknownAssigner));
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
}