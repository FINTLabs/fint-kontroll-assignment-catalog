package no.fintlabs.user;

import no.fintlabs.assignment.AssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private UserService userService;


    @Test
    void updateUser_shouldUpdateUserAndActivateAssignmentsWhenStatusChanged() {
        User user = mock(User.class);
        User updatedUser = mock(User.class);

        when(user.convertedUserEquals(updatedUser)).thenReturn(false);
        when(user.hasStatusChanged(updatedUser)).thenReturn(true);
        when(userRepository.save(updatedUser)).thenReturn(updatedUser);

        User result = userService.updateUser(user, updatedUser);

        assertEquals(updatedUser, result);
//        verify(updatedUser).setStatusChanged(any(Date.class));
        verify(userRepository).save(updatedUser);
        verify(assignmentService).activateOrDeactivateAssignmentsByUser(updatedUser);
    }

    @Test
    void updateUser_shouldUpdateUserWithoutActivatingAssignmentsWhenStatusNotChanged() {
        User user = mock(User.class);
        User updatedUser = mock(User.class);

        when(user.convertedUserEquals(updatedUser)).thenReturn(false);
        when(user.hasStatusChanged(updatedUser)).thenReturn(false);
        when(userRepository.save(updatedUser)).thenReturn(updatedUser);

        User result = userService.updateUser(user, updatedUser);

        assertEquals(updatedUser, result);
//        verify(updatedUser).setStatusChanged(any(Date.class));
        verify(userRepository).save(updatedUser);
        verify(assignmentService, never()).activateOrDeactivateAssignmentsByUser(updatedUser);
    }

    @Test
    void updateUser_shouldReturnSameUserWhenNoChanges() {
        User user = mock(User.class);
        User updatedUser = mock(User.class);

        when(user.convertedUserEquals(updatedUser)).thenReturn(true);

        User result = userService.updateUser(user, updatedUser);

        assertEquals(user, result);
//        verify(updatedUser, never()).setStatusChanged(any(Date.class));
        verify(userRepository, never()).save(updatedUser);
        verify(assignmentService, never()).activateOrDeactivateAssignmentsByUser(updatedUser);
    }
}
