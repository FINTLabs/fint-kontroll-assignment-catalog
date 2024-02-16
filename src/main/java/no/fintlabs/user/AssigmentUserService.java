package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssigmentUserService {

    private final UserRepository userRepository;
    private final AssignmentService assignmentService;

    public AssigmentUserService(UserRepository userRepository, AssignmentService assignmentService) {
        this.userRepository = userRepository;
        this.assignmentService = assignmentService;
    }
    public Page<AssignmentUser> findBySearchCriteria(Long resourceId, Specification<User> spec, Pageable page){
        Page<AssignmentUser> assignmentUsers = userRepository.findAll(spec, page)
                .map(User::toAssignmentUser)
                .map(user ->  {
                    user.setAssignmentRef(assignmentService.getAssignmentRefForUserAssignment(user.getId(), resourceId));
                    user.setAssignerUsername(assignmentService.getAssignerUsernameForUserAssignment(user.getId(), resourceId));
                    user.setAssignerDisplayname(assignmentService.getAssignerDisplaynameForUserAssignment(user.getId(), resourceId));
                    return user;
                });
        return assignmentUsers;
    }
}

