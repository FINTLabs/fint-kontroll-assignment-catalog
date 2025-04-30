package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasIdentityProviderUserObjectId;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.memberShipIsActive;

@Slf4j
@Service
public class FlattenedAssignmentMembershipService {

    private final MembershipRepository membershipRepository;
    private final FlattenedAssignmentMapper flattenedAssignmentMapper;

    public FlattenedAssignmentMembershipService(MembershipRepository membershipRepository, FlattenedAssignmentMapper flattenedAssignmentMapper) {
        this.membershipRepository = membershipRepository;
        this.flattenedAssignmentMapper = flattenedAssignmentMapper;
    }

    public List<FlattenedAssignment> createFlattenedAssignmentsForNewRoleAssignment(Assignment assignment) {
        log.info("Creating flattened assignments for new role assignment with id: {}", assignment.getId());

        if (assignment.getRoleRef() == null) {
            log.warn("Assignment {} has no roleRef. No flattened assignment saved.", assignment.getAssignmentId());
            return new ArrayList<>();
        }

        List<Membership> activeMemberships = membershipRepository.findAll(
                hasRoleId(assignment.getRoleRef())
                        .and(memberShipIsActive())
                        .and(hasIdentityProviderUserObjectId()));

        if (activeMemberships.isEmpty()) {
            log.warn("Role (group) has no active memberships. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
            return new ArrayList<>();
        } else {
            return mapNewAssignmentToFlattenedAssignments(activeMemberships, assignment);
        }
    }

    public List<FlattenedAssignment> createOrUpdateFlattenedAssignmentsForExistingAssignment(
            Assignment assignment,
            List<FlattenedAssignment> existingAssignments
    ) {
        log.info("Creating or updating flattened assignments for existing assignment with id: {}", assignment.getId());

        if (assignment.getRoleRef() == null) {
            log.warn("Assignment {} has no roleRef. No flattened assignment saved.", assignment.getAssignmentId());
            return new ArrayList<>();
        }

        List<Membership> memberships = membershipRepository.findAll(
                hasRoleId(assignment.getRoleRef())
                .and(hasIdentityProviderUserObjectId()));

        if (memberships.isEmpty()) {
            log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
            return new ArrayList<>();
        } else {
            return mapMembershipsForAssignmentToFlattenedAssignments(memberships, assignment, existingAssignments);
        }
    }

    private List<FlattenedAssignment> mapNewAssignmentToFlattenedAssignments(List<Membership> memberships, Assignment assignment) {
        log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", memberships.size(), assignment.getRoleRef());

        long start = System.currentTimeMillis();

        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        for (Membership membership : memberships) {
            flattenedAssignments.add(mapToFlattenedAssignment(membership, assignment));
        }

        long end = System.currentTimeMillis();
        log.info("Time taken {} ms to process {} memberships. Added {} to save", (end - start), memberships.size(), flattenedAssignments.size());

        return flattenedAssignments;
    }

    private List<FlattenedAssignment> mapMembershipsForAssignmentToFlattenedAssignments(
            List<Membership> memberships,
            Assignment assignment,
            List<FlattenedAssignment> existingAssignments
    ) {
        log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", memberships.size(), assignment.getRoleRef());
        long start = System.currentTimeMillis();

        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        for (Membership membership : memberships) {
            FlattenedAssignment mappedAssignment =mapToFlattenedAssignment(membership, assignment);
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments) //, isSync
                    .ifPresent(flattenedAssignments::add);
        }

        long end = System.currentTimeMillis();
        log.info("Time taken {} ms to process {} memberships. Added {} to save", (end - start), memberships.size(), flattenedAssignments.size());

        return flattenedAssignments;
    }

    private FlattenedAssignment mapToFlattenedAssignment(Membership membership, Assignment assignment) {
        boolean isActive = membership.getMemberStatus().equalsIgnoreCase("active");

        log.info("Mapping {} membership {} to flattened assignment for assignment with id {}",
                isActive ? "active" : "non active",
                membership.getId(),
                assignment.getId());

        FlattenedAssignment flattenedAssignment = toFlattenedAssignment(assignment);
        flattenedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
        flattenedAssignment.setUserRef(membership.getMemberId());

        if (!isActive) {
            Date assignmentRemovedDate = new Date();
            log.info("Flattened assignment removed date is set to {}", assignmentRemovedDate);
            flattenedAssignment.setAssignmentTerminationDate(assignmentRemovedDate);
        }
        return flattenedAssignment;
    }
}
