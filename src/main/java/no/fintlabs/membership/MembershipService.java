package no.fintlabs.membership;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.AssignmentService;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<Membership> getMembersAssignedToRole(Specification specification) {
        return membershipRepository.findAll(specification);
    }

    public List<Membership> getRolesAssignedToMember(Specification specification) {
        return membershipRepository.findAll(specification);
    }

    public void syncAssignmentsForMemberships(List<String> membershipIds) {
        membershipRepository.findAllById(membershipIds).forEach(this::processAssignmentsForMembership);
    }

    @Async
    public void processAssignmentsForMembership(Membership savedMembership) {
        log.info("Processing assignments for membership, roleId {}, memberId {}, id {}", savedMembership.getRoleId(), savedMembership.getMemberId(), savedMembership.getId());

        if (savedMembership.getIdentityProviderUserObjectId() == null) {
            log.info("Membership does not have identityProviderUserObjectId, skipping assignment processing, roleId {}, memberId {}, id {}", savedMembership.getRoleId(), savedMembership.getMemberId(),
                     savedMembership.getId());
            return;
        }

        assignmentService.getAssignmentsByRole(savedMembership.getRoleId())
                .forEach(assignment -> {
                    try {
                        flattenedAssignmentService.createFlattenedAssignmentsForMembership(assignment, savedMembership);
                    } catch (Exception e) {
                        log.error("Error processing assignments for membership, roledId {}, memberId {}, assignment {}",
                                  savedMembership.getRoleId(), savedMembership.getMemberId(), assignment.getId(), e);
                    }
                });
    }
}
