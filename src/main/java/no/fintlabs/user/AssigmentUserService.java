package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssigmentUserService {

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;

    public AssigmentUserService(UserRepository userRepository, AssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<AssignmentUser> getUsersAssignedToResource(Long resourceId) {
        List<AssignmentUser> users = userRepository
                .getUsersByResourceId(resourceId)
                .stream()
                .map(User::toSimpleUser)
                .map(user ->  {
                    user.setAssignmentRef(getAssignmentRef(user.getId(), resourceId));
                    return user;
                })
                .toList();
        return users;

    }
    private Long getAssignmentRef(Long userId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByUserRefAndResourceRef(userId, resourceId);

        if (assignment.isPresent()) {
            //return Optional.of(assignment.get().getId());
            return assignment.get().getId();
        }
        return null;
    }
}

