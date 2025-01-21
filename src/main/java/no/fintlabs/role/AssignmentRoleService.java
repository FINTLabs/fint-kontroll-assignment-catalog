package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssignmentRoleService {
    private final RoleRepository roleRepository;
    private final AssignmentService assignmentService;

    public AssignmentRoleService(RoleRepository roleRepository, AssignmentService assignmentService) {
        this.roleRepository = roleRepository;
        this.assignmentService = assignmentService;
    }

    public Page<AssignmentRole> findBySearchCriteria(Long resourceId, Specification<Role> specification, Pageable page) {

        return roleRepository.findAll(specification, page)
                .map(Role::toAssignmentRole)
                .map(role ->  {
                    assignmentService.getAssignmentRefForRoleAssignment(role.getId(), resourceId).ifPresent(role::setAssignmentRef);
                    assignmentService.getAssignerUsernameForRoleAssignment(role.getId(), resourceId).ifPresent(role::setAssignerUsername);
                    assignmentService.getAssignerDisplaynameForRoleAssignment(role.getId(), resourceId).ifPresent(role::setAssignerDisplayname);
                    return role;
                });
    }
}

