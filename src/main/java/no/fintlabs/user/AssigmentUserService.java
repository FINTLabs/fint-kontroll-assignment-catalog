package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
                    assignmentService.getAssignmentRefForUserAssignment(user.getId(), resourceId).ifPresent(user::setAssignmentRef);
                    assignmentService.getAssignerUsernameForUserAssignment(user.getId(), resourceId).ifPresent(user::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForUserAssignment(user.getId(), resourceId).ifPresent(user::setAssignerDisplayname);
                    return user;
                });
        return assignmentUsers;
    }
}

