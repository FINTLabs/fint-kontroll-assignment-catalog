package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private Long getAssignmentRef(Long userid, Long resourceId) {
        return assignmentRepository.findAssignmentByUserRefAndResourceRef(userid, resourceId).getId();
    }
}

