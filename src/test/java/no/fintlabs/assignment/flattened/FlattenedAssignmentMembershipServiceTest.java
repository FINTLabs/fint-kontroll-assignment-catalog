package no.fintlabs.assignment.flattened;

import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.MembershipSpecificationBuilder;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.fintlabs.assignment.AssignmentMapper.toFlattenedAssignment;
import static no.fintlabs.assignment.MembershipSpecificationBuilder.hasRoleId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlattenedAssignmentMembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private FlattenedAssignmentMapper flattenedAssignmentMapper;

    @InjectMocks
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;

    private Assignment assignment;
    private List<FlattenedAssignment> existingAssignments;

    @BeforeEach
    void setUp() {
        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setAssignmentId("assignmentId");
        assignment.setRoleRef(123L);
        assignment.setAzureAdGroupId(UUID.randomUUID());

        existingAssignments = new ArrayList<>();
    }

    @Test
    void findMembershipsToCreateOrUpdate_shouldReturnEmptyListWhenNoMemberships() {
        when(membershipRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>());

        List<FlattenedAssignment> result = flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, false);

        assertEquals(0, result.size());
    }

    @Test
    void findMembershipsToCreateOrUpdate_shouldSkipMembershipsWithoutIdentityProviderUserObjectId() {
        Membership membership = new Membership();
        membership.setId("123_1");
        membership.setIdentityProviderUserObjectId(null);
        membership.setMemberStatus("active");
        List<Membership> memberships = List.of(membership);

        when(membershipRepository.findAll(any(Specification.class))).thenReturn(memberships);

        List<FlattenedAssignment> result = flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, false);

        assertEquals(0, result.size());
    }

    @Test
    void findMembershipsToCreateOrUpdate_shouldSetAssignmentRemovedDateForInactiveMemberships() {

        Date memberStatusChangedDate = new Date();
        Membership membership = new Membership();
        membership.setId("123_1");
        membership.setIdentityProviderUserObjectId(UUID.randomUUID());
        membership.setMemberStatus("inactive");
        membership.setMemberStatusChanged(memberStatusChangedDate);
        List<Membership> memberships = List.of(membership);

        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();
        flattenedAssignment.setAssignmentTerminationDate(memberStatusChangedDate);

        when(membershipRepository.findAll(any(Specification.class))).thenReturn(memberships);
        when(flattenedAssignmentMapper.mapOriginWithExisting(any(), any(), anyBoolean())).thenReturn(Optional.of(flattenedAssignment));

        List<FlattenedAssignment> result = flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, false);


        assertEquals(1, result.size());
        assertEquals(membership.getMemberStatusChanged(), result.getFirst().getAssignmentTerminationDate());
    }

    @Test
    void findMembershipsToCreateOrUpdate_shouldMapAndAddFlattenedAssignments() {
        Membership membership = new Membership();
        membership.setId("123_1");
        membership.setIdentityProviderUserObjectId(UUID.randomUUID());
        membership.setMemberStatus("active");
        membership.setMemberId(123L);
        List<Membership> memberships = List.of(membership);

        FlattenedAssignment mappedAssignment = toFlattenedAssignment(assignment);
        mappedAssignment.setIdentityProviderUserObjectId(membership.getIdentityProviderUserObjectId());
        mappedAssignment.setUserRef(membership.getMemberId());

        when(membershipRepository.findAll(any(Specification.class))).thenReturn(memberships);
        when(flattenedAssignmentMapper.mapOriginWithExisting(any(), any(), anyBoolean())).thenReturn(Optional.of(mappedAssignment));

        List<FlattenedAssignment> result = flattenedAssignmentMembershipService.createOrUpdateFlattenedAssignmentsForExistingAssignment(assignment, existingAssignments, false);

        assertEquals(1, result.size());
        verify(flattenedAssignmentMapper, times(1)).mapOriginWithExisting(any(), any(), anyBoolean());
    }
}