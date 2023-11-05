package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
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
                .map(User::toSimpleUser)
                .map(user ->  {
                    user.setAssignmentRef(getAssignmentRef(user.getId(), resourceId));
                    return user;
                })
                .toList();
    }
    public Page<AssignmentUser> findBySearchCriteria(Specification<User> spec, Pageable page){
        List<AssignmentUser> assignmentUsers = userRepository.findAll(spec, page)
                .stream()
                .map(User::toSimpleUser)
                .toList();

        return new PageImpl<>(assignmentUsers);

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

