package no.fintlabs.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.membership.MembershipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Objects;
import java.util.Locale;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserService {
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final MembershipService membershipService;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(User existing, User updatedUser) {
        if (!existing.hasSamePersistedStateAs(updatedUser)) {
            User savedUser = saveUser(updatedUser);

            if (statusChanged(existing, updatedUser) && isInactive(updatedUser)) {
                assignmentService.deactivateAssignmentsByUserId(updatedUser.getId());
            }

            if (!updatedUser.getIdentityProviderUserObjectId().equals(existing.getIdentityProviderUserObjectId())) {
                assignmentService.updateAllAssignmentsOnUserChange(savedUser);
                membershipService.updateUserMemberships(savedUser);
            }
            return savedUser;
        } else {
            return existing;
        }
    }

    public boolean statusChanged(User existing, User incoming) {
        return !incoming.getStatus().equalsIgnoreCase(existing.getStatus());
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    private boolean isInactive(User user) {
        return !ACTIVE_STATUS.equalsIgnoreCase(user.getStatus());
    }

    @Transactional
    public void deactivateAssignmentsAndDeleteUser(Long userId) {
        assignmentService.deactivateAssignmentsByUserId(userId);
        userRepository.deleteById(userId);
    }
}
