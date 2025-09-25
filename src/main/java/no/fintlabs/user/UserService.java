package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.role.AssignmentRoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public Page<User> findBySearchCriteria(Specification<User> spec, Pageable page) {
        return userRepository.findAll(spec, page);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(User user, User updatedUser) {
        if (!user.convertedUserEquals(updatedUser)) {
            User savedUser = saveUser(updatedUser);

            if (user.hasStatusChanged(updatedUser)) {
                assignmentService.deactivateAssignmentsByUser(updatedUser);
            }
            if (!updatedUser.getIdentityProviderUserObjectId().equals(user.getIdentityProviderUserObjectId())) {
                assignmentService.updateAllAssignmentsOnUserChange(savedUser);
                membershipService.updateUserMemberships(savedUser);
            }
            return savedUser;
        } else {
            return user;
        }
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
