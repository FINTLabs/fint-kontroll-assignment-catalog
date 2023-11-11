package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public List<AssignmentUser> getUsersAssignedToResource(Long resourceId, String userType) {

        List<User> users = userType.equals("ALLTYPES")
                ? userRepository.getUsersByResourceId(resourceId)
                : userRepository.getUsersByResourceId(resourceId, userType);

        return users
                .stream()
                .map(User::toAssignmentUser)
                .map(user ->  {
                    user.setAssignmentRef(getAssignmentRef(user.getId(), resourceId));
                    return user;
                })
                .toList();
    }
    public Page<AssignmentUser> findBySearchCriteria(Long resourceId, Specification<User> spec, Pageable page){
        Page<AssignmentUser> assignmentUsers = userRepository.findAll(spec, page)
                .map(User::toAssignmentUser)
                .map(user ->  {
                    user.setAssignmentRef(getAssignmentRef(user.getId(), resourceId));
                    return user;
                });
        return assignmentUsers;
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

