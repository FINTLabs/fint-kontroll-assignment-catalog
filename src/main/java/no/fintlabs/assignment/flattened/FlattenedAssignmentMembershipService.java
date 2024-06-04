package no.fintlabs.assignment.flattened;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;

@Slf4j
@Service
public class FlattenedAssignmentMembershipService {
    private final MembershipRepository membershipRepository;
    private final FlattenedAssignmentMapper flattenedAssignmentMapper;

    public FlattenedAssignmentMembershipService(MembershipRepository membershipRepository, FlattenedAssignmentMapper flattenedAssignmentMapper) {
        this.membershipRepository = membershipRepository;
        this.flattenedAssignmentMapper = flattenedAssignmentMapper;
    }

    public List<FlattenedAssignment> findMembershipsToCreateOrUpdate(Assignment assignment, boolean isSync) {
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();
        List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

        if (memberships.isEmpty()) {
            log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
        } else {
            log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", memberships.size(), assignment.getRoleRef());

            for (int i = 0; i < memberships.size(); i++) {
                long start = System.currentTimeMillis();
                Membership membership = memberships.get(i);
                FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
                mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
                mappedAssignment.setUserRef(membership.getMemberId());
                flattenedAssignments.add(flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, isSync));
                log.info("Processing member index: " + i + " of " + memberships.size() + " for roleref: " + assignment.getRoleRef());
                long end = System.currentTimeMillis();
                log.info("Time taken to process member: " + (end - start) + " ms");
            }

            /*memberships.forEach(membership -> {
                FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
                mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
                mappedAssignment.setUserRef(membership.getMemberId());
                flattenedAssignments.add(flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, isSync));
            });*/

            log.info("Added: {} memberships/groups to save", flattenedAssignments.size());
        }

        return flattenedAssignments;
    }
}
