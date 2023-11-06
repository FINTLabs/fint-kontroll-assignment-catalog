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

    public List<AssignmentRole> getRolesAssignedToResource(Long resourceId) {
        List<AssignmentRole> roles = roleRepository
                .getRolesByResourceId(resourceId)
                .stream()
                .map(Role::toAssignmentRole)
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

    public Page<AssignmentRole> findBySearchCriteria(Long resourceId, Specification<Role> specification, Pageable page) {
        List<AssignmentRole> assignmentRoles = roleRepository.findAll(specification, page)
                .map(Role::toAssignmentRole)
                .map(role ->  {
                    role.setAssignmentRef(getAssignmentRef(role.getId(), resourceId));
                    return role;
                })
                .toList();

        return new PageImpl<>(assignmentRoles);

    }
}

