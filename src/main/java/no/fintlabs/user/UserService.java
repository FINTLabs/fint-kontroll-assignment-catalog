package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    public UserService(UserRepository userRepository, AssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
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
            updatedUser.setStatusChanged(new Date());
            User savedUser = saveUser(updatedUser);

            if (user.hasStatusChanged(updatedUser)) {
                assignmentService.activateOrDeactivateAssignmentsByUser(updatedUser);
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
