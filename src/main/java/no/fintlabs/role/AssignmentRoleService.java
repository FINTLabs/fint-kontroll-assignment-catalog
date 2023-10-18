package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentRepository;
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

    public List<AssignmentRole> getRolesAssignedToResource(Long resourceId) {
        List<AssignmentRole> roles = roleRepository
                .getRolesByResourceId(resourceId)
                .stream()
                .map(Role::toAssignemntRole)
                .map(role ->  {
                    role.setAssignmentRef(getAssignmentRef(role.getId(), resourceId));
                    return role;
                })
                .toList();
        return roles;
    }
    private Long getAssignmentRef(Long roleId, Long resourceId) {
        Optional<Assignment> assignment = assignmentRepository.findAssignmentByRoleRefAndResourceRef(roleId, resourceId);

        if (assignment.isPresent()) {
            //return Optional.of(assignment.get().getId());
            return assignment.get().getId();
        }
        return null;
    }
}

