package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;

@Slf4j
@Service
public class FlattenedAssignmentService {

    private final FlattenedAssignmentRepository flattenedAssignmentRepository;

    private final MembershipRepository membershipRepository;

    public FlattenedAssignmentService(FlattenedAssignmentRepository flattenedAssignmentRepository,
                                      MembershipRepository membershipRepository) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.membershipRepository = membershipRepository;
    }

    public void createFlattenedAssignments(Assignment assignment) {
        log.info("Creating flattened assignments for assignment with id {}", assignment.getId());

        if (assignment.getUserRef() != null) {
            flattenedAssignmentRepository.save(toFlattenedAssignment(assignment));
        } else if (assignment.getRoleRef() != null) {
            List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

            if (memberships.isEmpty()) {
                log.info("Role (group) has no members. Saving flattened assignment without members. Roleref: {}", assignment.getRoleRef());
                flattenedAssignmentRepository.save(toFlattenedAssignment(assignment));
            } else {
                log.info("Saving flattened assignments for roleref {}", assignment.getRoleRef());
                memberships.forEach(membership -> flattenedAssignmentRepository.save(toFlattenedAssignment(assignment)));
            }
        }
    }

    public List<FlattenedAssignment> getAllFlattenedAssignments() {
        return flattenedAssignmentRepository.findAll();
    }
}
