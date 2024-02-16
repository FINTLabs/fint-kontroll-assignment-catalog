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
        Page<AssignmentRole> assignmentRoles = roleRepository.findAll(specification, page)
                .map(Role::toAssignmentRole)
                .map(role ->  {
                    role.setAssignmentRef(assignmentService.getRoleAssignmentRef(role.getId(), resourceId));
                    role.setAssignerUsername(assignmentService.getAssignerUsernameForRoleAssignment(role.getId(), resourceId));
                    role.setAssignerDisplayname(assignmentService.getAssignerDisplaynameForRoleAssignment(role.getId(), resourceId));
                    return role;
                });
        return assignmentRoles;
    }

}

