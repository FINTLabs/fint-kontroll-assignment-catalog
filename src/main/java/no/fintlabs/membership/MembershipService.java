package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentMembershipService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.user.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    private final AssignmentService assignmentService;

    public MembershipService(MembershipRepository membershipRepository, FlattenedAssignmentService flattenedAssignmentService, FlattenedAssignmentMembershipService flattenedAssignmentMembershipService, AssignmentService assignmentService) {
        this.membershipRepository = membershipRepository;
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.flattenedAssignmentMembershipService = flattenedAssignmentMembershipService;
        this.assignmentService = assignmentService;
    }

    public Membership save(Membership membership) {
        return membershipRepository.save(membership);
    }

    public List<Membership> getMembersAssignedToRole(Specification<Membership> specification) {
        return membershipRepository.findAll(specification);
    }

    public List<Membership> getRolesAssignedToMember(Specification<Membership> specification) {
        return membershipRepository.findAll(specification);
    }

    public void syncAssignmentsForMemberships(List<String> membershipIds) {
        membershipRepository.findAllById(membershipIds).forEach(this::syncAssignmentsForMembership);
    }

    @Async
    public void deactivateFlattenedAssignmentsForMembership(Membership membership) {
        log.info("Find flattened assignments associated with inactive membership {} to be deactivated",
                membership.getId()
        );

        Set<Long> flattenedAssignmentIdsByUserAndRoleRef =
                flattenedAssignmentService.findFlattenedAssignmentIdsByUserAndRoleRef(membership.getMemberId(), membership.getRoleId());

        if (flattenedAssignmentIdsByUserAndRoleRef.isEmpty()) {
            log.info("No associated flattened assignments with inactive membership {} found for deactivation",
                    membership.getId());
            return;
        }
        log.info("Deactivating {} flattened assignments assigned via inactive membership {}",
                flattenedAssignmentIdsByUserAndRoleRef.size(),
                membership.getId()
        );
        flattenedAssignmentService.deactivateFlattenedAssignments(flattenedAssignmentIdsByUserAndRoleRef);
    }

    @Async
    public void syncAssignmentsForMembership(Membership savedMembership) {

        List<Assignment> assignmentsByRole = assignmentService.getAssignmentsByRole(savedMembership.getRoleId());

        if (assignmentsByRole.isEmpty()) {
            log.info("No assignments associated with roleId {}. No processing will be done for membership {}.",
                    savedMembership.getRoleId(),
                    savedMembership.getId()
            );
            return;
        }
        log.info("Processing assignments for membership, roleId {}, memberId {}, assignments {}",
                savedMembership.getRoleId(),
                savedMembership.getMemberId(),
                assignmentsByRole.size()
        );
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();
        assignmentsByRole
                .forEach(assignment -> {
                    try {
                        List<FlattenedAssignment> existingFlattenedAssignments =
                                flattenedAssignmentService.getFlattenedAssignmentsByAssignmentAndUserAndRoleAssignmentNotTerminated(
                                        assignment.getId(),
                                        savedMembership.getMemberId(),
                                        savedMembership.getRoleId()
                                );
                        flattenedAssignments.addAll(flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(
                                assignment,
                                existingFlattenedAssignments
                        ));
                    } catch (Exception e) {
                        log.error("Error processing assignments for membership, roledId {}, memberId {}, assignment {}, error: {}", savedMembership.getRoleId(), savedMembership.getMemberId(), assignment.getId(), e.getMessage());
                    }
                });
        if (!flattenedAssignments.isEmpty()) {
            flattenedAssignmentService.saveAndPublishFlattenedAssignmentsBatch(flattenedAssignments, true);
        }
    }

    public void updateUserMemberships(User savedUser) {
        List<Membership> userMemberships = membershipRepository.findAllByMemberId(savedUser.getId());
        userMemberships.forEach(membership -> membership.setIdentityProviderUserObjectId(savedUser.getIdentityProviderUserObjectId()));
        membershipRepository.saveAll(userMemberships);
        log.info("Updated memberships for user with id {}", savedUser.getId());
    }

}
