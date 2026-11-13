package no.fintlabs.assignment.user.projection;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Testcontainers
public class FlattenedAssignmentRepositoryIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;

    private FlattenedAssignment flattenedAssignment1, flattenedAssignment2, flattenedAssignment3,
                                savedFlattenedAssignment1, savedFlattenedAssignment2, savedFlattenedAssignment3;


    @BeforeEach
    public void setUp() {
        UUID firstUserEntraId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID secondUserEntraId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID resourceEntraId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UserEntraMembership firstMembership = membership(firstUserEntraId, resourceEntraId);
        UserEntraMembership secondMembership = membership(secondUserEntraId, resourceEntraId);

        flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setAssignmentId(1L);
        flattenedAssignment1.setResourceRef(10L);
        flattenedAssignment1.setAssignmentViaRoleRef(2L);
        flattenedAssignment1.setIdentityProviderUserObjectId(firstUserEntraId);
        flattenedAssignment1.setIdentityProviderGroupObjectId(resourceEntraId);
        flattenedAssignment1.setUserEntraMembership(firstMembership);

        flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setAssignmentTerminationDate(new Date());
        flattenedAssignment2.setAssignmentTerminationReason("Assignment removed");
        flattenedAssignment2.setResourceRef(10L);
        flattenedAssignment2.setAssignmentViaRoleRef(2L);
        flattenedAssignment2.setIdentityProviderUserObjectId(secondUserEntraId);
        flattenedAssignment2.setIdentityProviderGroupObjectId(resourceEntraId);
        flattenedAssignment2.setUserEntraMembership(secondMembership);

        flattenedAssignment3 = new FlattenedAssignment();
        flattenedAssignment3.setUserRef(20L);
        flattenedAssignment3.setAssignmentId(2L);
        flattenedAssignment3.setResourceRef(10L);
        flattenedAssignment3.setAssignmentViaRoleRef(3L);
        flattenedAssignment3.setIdentityProviderUserObjectId(secondUserEntraId);
        flattenedAssignment3.setIdentityProviderGroupObjectId(resourceEntraId);
        flattenedAssignment3.setUserEntraMembership(secondMembership);

        savedFlattenedAssignment1 = flattenedAssignmentRepository.save(flattenedAssignment1);
        savedFlattenedAssignment2 = flattenedAssignmentRepository.save(flattenedAssignment2);
        savedFlattenedAssignment3 = flattenedAssignmentRepository.save(flattenedAssignment3);
    }

    @Test
    public void testFindByAssignmentIdAndAssignmentTerminationDateIsNull() {
        List<FlattenedAssignment> activeFlattenedAssignmentsForAssignment1L = flattenedAssignmentRepository.findByAssignmentIdAndAssignmentTerminationDateIsNull(1L);
        assertThat(activeFlattenedAssignmentsForAssignment1L.size()).isEqualTo(1);
        assertThat(activeFlattenedAssignmentsForAssignment1L.getFirst().getId()).isEqualTo(savedFlattenedAssignment1.getId());
    }

    @Test
    public void existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot_shouldFindRoleAssignmentWhenCurrentAssignmentIsDirect() {
        FlattenedAssignment directAssignment = new FlattenedAssignment();
        directAssignment.setUserRef(20L);
        directAssignment.setAssignmentId(3L);
        directAssignment.setResourceRef(10L);
        directAssignment.setAssignmentViaRoleRef(null);
        directAssignment.setAssignmentTerminationDate(new Date());
        directAssignment.setIdentityProviderUserObjectId(savedFlattenedAssignment3.getIdentityProviderUserObjectId());
        directAssignment.setIdentityProviderGroupObjectId(savedFlattenedAssignment3.getIdentityProviderGroupObjectId());
        directAssignment.setUserEntraMembership(savedFlattenedAssignment3.getUserEntraMembership());
        FlattenedAssignment savedDirectAssignment = flattenedAssignmentRepository.saveAndFlush(directAssignment);

        boolean otherActiveAssignments = flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(
                        savedDirectAssignment.getUserEntraMembership(),
                        savedDirectAssignment.getId()
                );

        assertThat(otherActiveAssignments).isTrue();
    }

    @Test
    public void existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot_shouldExcludeCurrentAssignment() {
        boolean otherActiveAssignments = flattenedAssignmentRepository
                .existsByUserEntraMembershipAndAssignmentTerminationDateIsNullAndIdNot(
                        savedFlattenedAssignment1.getUserEntraMembership(),
                        savedFlattenedAssignment1.getId()
                );

        assertThat(otherActiveAssignments).isFalse();
    }

    private UserEntraMembership membership(UUID userEntraId, UUID resourceEntraId) {
        return UserEntraMembership.builder()
                .userEntraId(userEntraId)
                .resourceEntraId(resourceEntraId)
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();
    }
}
