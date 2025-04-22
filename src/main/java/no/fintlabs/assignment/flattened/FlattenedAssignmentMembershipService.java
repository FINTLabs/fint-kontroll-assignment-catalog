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

    public List<FlattenedAssignment> findMembershipsToCreateOrUpdate(Assignment assignment, List<FlattenedAssignment> existingAssignments, boolean isSync) {
        List<FlattenedAssignment> flattenedAssignments = new ArrayList<>();
        List<Membership> memberships = membershipRepository.findAll(hasRoleId(assignment.getRoleRef()));

        if (memberships.isEmpty()) {
            log.warn("Role (group) has no members. No flattened assignment saved. Roleref: {}", assignment.getRoleRef());
        } else {
            log.info("Preparing all {} memberships to save as flattened assignments for roleref {}", memberships.size(), assignment.getRoleRef());
            long start = System.currentTimeMillis();

            for (int i = 0; i < memberships.size(); i++) {
                Membership membership = memberships.get(i);

                if(membership.getIdentityProviderUserObjectId() == null) {
                    log.warn("Membership with id {} has no identityProviderUserObjectId. Skipping", membership.getId());
                    continue;
                }

                FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
                mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
                mappedAssignment.setUserRef(membership.getMemberId());

                if (!membership.getMemberStatus().equalsIgnoreCase("active")) {
                    //TODO: maybe just use new Date()
                    Date assignmentRemovedDate = membership.getMemberStatusChanged()!=null ? membership.getMemberStatusChanged() : new Date();
                    log.info("Membership with id {} has non active member status. Flattened assignment removed date is set to {}", membership.getId(), assignmentRemovedDate);
                    mappedAssignment.setAssignmentTerminationDate(assignmentRemovedDate);
                }

               flattenedAssignmentMapper.mapOriginWithExisting(mappedAssignment, existingAssignments, isSync)
                        .ifPresent(flattenedAssignments::add);
            }

            long end = System.currentTimeMillis();
            log.info("Time taken {}ms to process {} memberships. Added {} to save", (end - start), memberships.size(), flattenedAssignments.size());
        }

        return flattenedAssignments;
    }
}
