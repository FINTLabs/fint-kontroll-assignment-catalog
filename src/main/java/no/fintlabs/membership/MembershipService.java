package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.user.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssignmentService assignmentService;

    public MembershipService(MembershipRepository membershipRepository, FlattenedAssignmentService flattenedAssignmentService, AssignmentService assignmentService) {
        this.membershipRepository = membershipRepository;
        this.flattenedAssignmentService = flattenedAssignmentService;
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
        List<String> distinctMembershipIds = new ArrayList<>(new LinkedHashSet<>(membershipIds));
        List<Membership> memberships = membershipRepository.findAllById(distinctMembershipIds);
        Set<String> foundMembershipIds = memberships.stream().map(Membership::getId).collect(Collectors.toSet());
        Set<String> missingMembershipIds = new LinkedHashSet<>(distinctMembershipIds);
        missingMembershipIds.removeAll(foundMembershipIds);
        if (!missingMembershipIds.isEmpty()) {
            throw new ResourceNotFoundException("Memberships not found: " + missingMembershipIds);
        }

        Map<Long, List<Assignment>> assignmentsByRoleId = assignmentService.getAssignmentsByRoles(
                        memberships.stream().map(Membership::getRoleId).distinct().toList())
                .stream()
                .collect(Collectors.groupingBy(Assignment::getRoleRef));

        memberships.forEach(membership -> syncAssignmentsForMembership(
                membership,
                assignmentsByRoleId.getOrDefault(membership.getRoleId(), List.of())));
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

    public void syncAssignmentsForMembership(Membership savedMembership) {
        syncAssignmentsForMembership(savedMembership, assignmentService.getAssignmentsByRole(savedMembership.getRoleId()));
    }

    private void syncAssignmentsForMembership(Membership savedMembership, List<Assignment> assignmentsByRole) {
        if (savedMembership.getIdentityProviderUserObjectId() == null) {
            log.info("Membership does not have identityProviderUserObjectId, skipping assignment processing, roleId {}, memberId {}, id {}",
                    savedMembership.getRoleId(),
                    savedMembership.getMemberId(),
                    savedMembership.getId()
            );
            return;
        }

        if (!assignmentsByRole.isEmpty()) {
            log.info("Processing assignments for membership, roleId {}, memberId {}, assignments {}", savedMembership.getRoleId(), savedMembership.getMemberId(), assignmentsByRole.size());
        }

        assignmentsByRole.forEach(assignment ->
                flattenedAssignmentService.createOrUpdateFlattenedAssignmentsForMembership(assignment, savedMembership));
    }

    public void updateUserMemberships(User savedUser) {
        List<Membership> userMemberships = membershipRepository.findAllByMemberId(savedUser.getId());
        userMemberships.forEach(membership -> membership.setIdentityProviderUserObjectId(savedUser.getIdentityProviderUserObjectId()));
        membershipRepository.saveAll(userMemberships);
        log.info("Updated memberships for user with id {}", savedUser.getId());
    }

}
