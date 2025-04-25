package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.*;

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

        if (assignment.getRoleRef()==null) {
            log.warn("Assignment {} has no roleRef. No flattened assignment saved.", assignment.getAssignmentId());
            return new ArrayList<>();
        }
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        Specification<Membership> activeAndValidMembershipSpecification = hasRoleId(assignment.getRoleRef())
                .and(memberShipIsActive())
                .and(hasIdentityProviderUserObjectId());

        List<Membership> activeMemberships = membershipRepository.findAll(activeAndValidMembershipSpecification);

        if (activeMemberships.isEmpty()) {
            log.warn("Role (group) has no active memberships. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
        }
        else {
            log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", activeMemberships.size(), assignment.getRoleRef());

            long start = System.currentTimeMillis();
            flattenedAssignments = mapNewAssignmentToFlattenedAssignments(activeMemberships, assignment);
            long end = System.currentTimeMillis();
            log.info("Time taken {}ms to process {} memberships. Added {} to save", (end - start), activeMemberships.size(), flattenedAssignments.size());
        }

        return flattenedAssignments;
    }

    public List<FlattenedAssignment> createOrUpdateFlattenedAssignmentsForExistingAssignment(
            Assignment assignment,
            List<FlattenedAssignment> existingAssignments,
            boolean isSync
    ) {
        if (assignment.getRoleRef()==null) {
            log.warn("Assignment {} has no roleRef. No flattened assignment saved.", assignment.getAssignmentId());
            return new ArrayList<>();
        }
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        Specification<Membership> validMembershipSpecification = hasRoleId(assignment.getRoleRef())
                .and(hasIdentityProviderUserObjectId());

        List<Membership> memberships = membershipRepository.findAll(validMembershipSpecification);

        if (memberships.isEmpty()) {
            log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
        }
        else {
            log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", memberships.size(), assignment.getRoleRef());
            long start = System.currentTimeMillis();
            flattenedAssignments = mapExistingAssignmentToFlattenedAssignments(memberships, assignment, existingAssignments, isSync);
            long end = System.currentTimeMillis();
            log.info("Time taken {}ms to process {} memberships. Added {} to save", (end - start), memberships.size(), flattenedAssignments.size());
        }

        return flattenedAssignments;
    }

    private List<FlattenedAssignment> mapNewAssignmentToFlattenedAssignments(List<Membership> memberships, Assignment assignment) {
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        for (Membership membership : memberships) {
            flattenedAssignments.add(mapToFlattenedAssignment(membership, assignment));
        }
        return flattenedAssignments;
    }

    private List<FlattenedAssignment> mapExistingAssignmentToFlattenedAssignments(
            List<Membership> memberships,
            Assignment assignment,
            List<FlattenedAssignment> existingAssignments,
            boolean isSync
    ) {
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();

        for (Membership membership : memberships) {
            FlattenedAssignment mappedAssignment =mapToFlattenedAssignment(membership, assignment);

            if (!membership.getMemberStatus().equalsIgnoreCase("active")) {
                Date assignmentRemovedDate =  new Date();
                log.info("Membership with id {} has non active member status. Flattened assignment removed date is set to {}", membership.getId(), assignmentRemovedDate);
                mappedAssignment.setAssignmentTerminationDate(assignmentRemovedDate);
            }
            flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments, isSync)
                    .ifPresent(flattenedAssignments::add);
        }
        return flattenedAssignments;
    }

    private FlattenedAssignment mapToFlattenedAssignment(Membership membership, Assignment assignment) {
        FlattenedAssignment flattenedAssignment = toFlattenedAssignment(assignment);
        flattenedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
        flattenedAssignment.setUserRef(membership.getMemberId());

        return flattenedAssignment;
    }
}
