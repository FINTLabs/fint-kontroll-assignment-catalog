package no.fintlabs.assignment.user.projection;

import no.fintlabs.DatabaseIntegrationTest;
import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.Assignment;
import no.fintlabs.assignment.user.entra.UserEntraMembership;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepository;
import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import no.fintlabs.enforcement.LicenseEnforcementService;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipRepository;
import no.fintlabs.opa.OpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

@DataJpaTest
@Testcontainers
@Import({FlattenedAssignmentService.class,
        FlattenedAssignmentMapper.class
})
public class FlattenedAssignmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Autowired
    private FlattenedAssignmentService flattenedAssignmentService;
    @Autowired
    private FlattenedAssignmentRepository flattenedAssignmentRepository;
    @Autowired
    private UserEntraMembershipRepository userEntraMembershipRepository;
    @MockBean
    private FlattenedAssignmentMembershipService flattenedAssignmentMembershipService;
    @MockBean
    private UserEntraMembershipPublisher assigmentEntityProducerService;
    @MockBean
    private LicenseEnforcementService licenseEnforcementService;
//    @MockBean
//    private OpaService opaService;


    private FlattenedAssignment flattenedAssignment1, flattenedAssignment2, flattenedAssignment3, flattenedAssignment4,
            savedFlattenedAssignment1, savedFlattenedAssignment2, savedFlattenedAssignment3, savedFlattenedAssignment4;


    @BeforeEach
    public void setUp() {

        flattenedAssignment1 = new FlattenedAssignment();
        flattenedAssignment1.setUserRef(10L);
        flattenedAssignment1.setAssignmentId(1L);
        flattenedAssignment1.setResourceRef(10L);
        flattenedAssignment1.setAssignmentViaRoleRef(2L);
        flattenedAssignment1.setAssignmentTerminationDate(new Date());
        flattenedAssignment1.setIdentityProviderUserObjectId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        flattenedAssignment1.setIdentityProviderGroupObjectId(UUID.fromString("20000000-0000-0000-0000-000000000001"));

        flattenedAssignment2 = new FlattenedAssignment();
        flattenedAssignment2.setUserRef(20L);
        flattenedAssignment2.setAssignmentId(1L);
        flattenedAssignment2.setResourceRef(10L);
        flattenedAssignment2.setAssignmentViaRoleRef(2L);
        flattenedAssignment2.setIdentityProviderUserObjectId(UUID.fromString("10000000-0000-0000-0000-000000000002"));
        flattenedAssignment2.setIdentityProviderGroupObjectId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
        flattenedAssignment2.setUserEntraMembership(UserEntraMembership.builder()
                .userEntraId(flattenedAssignment2.getIdentityProviderUserObjectId())
                .resourceEntraId(flattenedAssignment2.getIdentityProviderGroupObjectId())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build());

        flattenedAssignment3 = new FlattenedAssignment();
        flattenedAssignment3.setUserRef(10L);
        flattenedAssignment3.setAssignmentId(2L);
        flattenedAssignment3.setResourceRef(10L);
        flattenedAssignment3.setAssignmentViaRoleRef(3L);
        flattenedAssignment3.setIdentityProviderUserObjectId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        flattenedAssignment3.setIdentityProviderGroupObjectId(UUID.fromString("20000000-0000-0000-0000-000000000001"));

        UserEntraMembership sharedMembership = UserEntraMembership.builder()
                .userEntraId(flattenedAssignment1.getIdentityProviderUserObjectId())
                .resourceEntraId(flattenedAssignment1.getIdentityProviderGroupObjectId())
                .membershipStatus(MembershipStatus.ACTIVE)
                .entraStatus(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED)
                .build();
        sharedMembership.addFlattenedAssignment(flattenedAssignment1);
        sharedMembership.addFlattenedAssignment(flattenedAssignment3);

        flattenedAssignment4 = new FlattenedAssignment();
        flattenedAssignment4.setUserRef(10L);
        flattenedAssignment4.setAssignmentId(3L);
        flattenedAssignment4.setResourceRef(10L);
        flattenedAssignment4.setIdentityProviderUserObjectId(flattenedAssignment1.getIdentityProviderUserObjectId());
        flattenedAssignment4.setIdentityProviderGroupObjectId(flattenedAssignment1.getIdentityProviderGroupObjectId());

        savedFlattenedAssignment1 = flattenedAssignmentRepository.save(flattenedAssignment1);
        savedFlattenedAssignment2 = flattenedAssignmentRepository.save(flattenedAssignment2);
    }

    @Test
    public void shouldPublishDeletionWhenNoOtherActiveAssignmentsExist() {
        flattenedAssignment3.setAssignmentTerminationDate(new Date());
        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment3);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment1));
        FlattenedAssignment deactivatedFlattenedAssignment = flattenedAssignmentRepository.findById(savedFlattenedAssignment1.getId()).orElseThrow();

        assertThat(deactivatedFlattenedAssignment.getUserEntraMembership().getEntraStatus())
                .isNotEqualTo(EntraMembershipSyncStatus.DELETION_CONFIRMED);
        verify(assigmentEntityProducerService, times(1)).publishDeletion(flattenedAssignment1);
    }

    @Test
    public void shouldKeepMembershipWhenAnotherActiveGroupAssignmentExists() {

        savedFlattenedAssignment3 = flattenedAssignmentRepository.save(flattenedAssignment3);
        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment1));

        verify(assigmentEntityProducerService, never()).publishDeletion(flattenedAssignment1);
    }

    @Test
    public void shouldNotPublishDirectAssignmentDeletionWhenRoleAssignmentStillExists() {
        savedFlattenedAssignment3 = flattenedAssignmentRepository.saveAndFlush(flattenedAssignment3);
        FlattenedAssignment directAssignment = new FlattenedAssignment();
        directAssignment.setUserRef(10L);
        directAssignment.setAssignmentId(3L);
        directAssignment.setResourceRef(10L);
        directAssignment.setAssignmentViaRoleRef(null);
        directAssignment.setAssignmentTerminationDate(new Date());
        directAssignment.setIdentityProviderUserObjectId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        directAssignment.setIdentityProviderGroupObjectId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
        directAssignment.setUserEntraMembership(savedFlattenedAssignment3.getUserEntraMembership());

        FlattenedAssignment savedDirectAssignment = flattenedAssignmentRepository.saveAndFlush(directAssignment);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(savedDirectAssignment));

        verify(assigmentEntityProducerService, never()).publishDeletion(savedDirectAssignment);
        verify(assigmentEntityProducerService, never()).publishDeletion(flattenedAssignment1);
    }

    @Test
    public void shouldKeepMembershipWhenAnotherActiveDirectAssignmentExists() {
        flattenedAssignment3.setAssignmentTerminationDate(new Date());
        flattenedAssignmentRepository.saveAndFlush(flattenedAssignment3);

        flattenedAssignment4.setUserEntraMembership(savedFlattenedAssignment1.getUserEntraMembership());
        savedFlattenedAssignment4 = flattenedAssignmentRepository.save(flattenedAssignment4);

        flattenedAssignmentService.publishDeactivatedFlattenedAssignmentsForDeletion(List.of(flattenedAssignment1));

        FlattenedAssignment deactivatedFlattenedAssignment = flattenedAssignmentRepository.findById(savedFlattenedAssignment1.getId()).orElseThrow();
        FlattenedAssignment activeFlattenedAssignment = flattenedAssignmentRepository.findById(savedFlattenedAssignment4.getId()).orElseThrow();

        assertThat(deactivatedFlattenedAssignment.getUserEntraMembership().getMembershipStatus())
                .isEqualTo(MembershipStatus.ACTIVE);
        assertThat(activeFlattenedAssignment.getUserEntraMembership().getEntraStatus())
                .isEqualTo(EntraMembershipSyncStatus.MEMBERSHIP_CONFIRMED);

        verify(assigmentEntityProducerService, never()).publishDeletion(flattenedAssignment1);
    }
}
