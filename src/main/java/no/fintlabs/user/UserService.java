package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.role.AssignmentRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final AssignmentRoleService assignmentRoleService;
    private final MembershipService membershipService;

    public UserService(UserRepository userRepository, AssignmentService assignmentService, AssignmentRoleService assignmentRoleService, MembershipService membershipService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
        this.assignmentRoleService = assignmentRoleService;
        this.membershipService = membershipService;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(User existing, User updatedUser) {
        if (!existing.convertedUserEquals(updatedUser)) {
            User savedUser = saveUser(updatedUser);

            if (statusChanged(existing, updatedUser) && !updatedUser.getStatus().equalsIgnoreCase("ACTIVE")) {
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
        return existing.getStatus() != null && !existing.getStatus().equalsIgnoreCase(incoming.getStatus());
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateAssignmentsAndDeleteUser(Long userId) {
        assignmentService.deactivateAssignmentsByUserId(userId);
        userRepository.deleteById(userId);
    }
}
