package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
import no.fintlabs.role.AssignmentRole;
import no.fintlabs.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AssignmentRoleService {
    private final RoleRepository roleRepository;
    private final AssignmentRepository assignmentRepository;

    public AssignmentRoleService(RoleRepository roleRepository, AssignmentRepository assignmentRepository) {
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
    }
    public Page<AssignmentRole> findBySearchCriteria(Long resourceId, Specification<Role> specification, Pageable page) {
        Page<AssignmentRole> assignmentRoles = roleRepository.findAll(specification, page)
                .map(Role::toAssignmentRole)
                .map(role ->  {
                    role.setAssignmentRef(getAssignmentRef(role.getId(), resourceId));
                    return role;
                });
        return assignmentRoles;
    }
    private Long getAssignmentRef(Long roleId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByRoleRefAndResourceRef(roleId, resourceId);

        if (assignment.isPresent()) {
            return assignment.get().getId();
        }
        return null;
    }
}

