package no.fintlabs.assignment.user.entra;

import no.fintlabs.entra.membership.EntraMembershipSyncStatus;
import no.fintlabs.entra.membership.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserEntraMembershipRepositoryTest {

    @Autowired
    private UserEntraMembershipRepository repository;

    @Test
    void retriesUnansweredMessagesAfter72HoursAndFailuresAfter24Hours() {
        Instant now = Instant.now();
        Date responseCutoff = Date.from(now.minus(Duration.ofHours(72)));
        Date failedCutoff = Date.from(now.minus(Duration.ofHours(24)));

        UserEntraMembership oldAddition = membership(MembershipStatus.ACTIVE, EntraMembershipSyncStatus.SENT);
        oldAddition.setSentToEntraAt(Date.from(now.minus(Duration.ofHours(73))));
        UserEntraMembership recentAddition = membership(MembershipStatus.ACTIVE, EntraMembershipSyncStatus.SENT);
        recentAddition.setSentToEntraAt(Date.from(now.minus(Duration.ofHours(71))));
        UserEntraMembership oldDeletion = membership(MembershipStatus.INACTIVE, EntraMembershipSyncStatus.DELETION_SENT);
        oldDeletion.setDeletionSentToEntraAt(Date.from(now.minus(Duration.ofHours(73))));
        UserEntraMembership recentDeletion = membership(MembershipStatus.INACTIVE, EntraMembershipSyncStatus.DELETION_SENT);
        recentDeletion.setDeletionSentToEntraAt(Date.from(now.minus(Duration.ofHours(71))));
        UserEntraMembership oldFailure = membership(MembershipStatus.ACTIVE, EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        oldFailure.setSentToEntraAt(Date.from(now.minus(Duration.ofHours(25))));
        UserEntraMembership recentActiveFailure = membership(MembershipStatus.ACTIVE, EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        recentActiveFailure.setSentToEntraAt(Date.from(now.minus(Duration.ofHours(23))));
        UserEntraMembership oldInactiveFailure = membership(MembershipStatus.INACTIVE, EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        oldInactiveFailure.setDeletionSentToEntraAt(Date.from(now.minus(Duration.ofHours(25))));
        UserEntraMembership recentFailure = membership(MembershipStatus.INACTIVE, EntraMembershipSyncStatus.NEEDS_REPUBLISH);
        recentFailure.setDeletionSentToEntraAt(Date.from(now.minus(Duration.ofHours(23))));
        UserEntraMembership unsent = membership(MembershipStatus.ACTIVE, EntraMembershipSyncStatus.NOT_SENT);

        repository.saveAllAndFlush(List.of(oldAddition, recentAddition, oldDeletion,
                recentDeletion, oldFailure, recentActiveFailure, oldInactiveFailure, recentFailure, unsent));

        List<Long> selectedIds = repository.findStaleMembershipsByIdGreaterThanOrderByIdAsc(
                List.of(EntraMembershipSyncStatus.NOT_SENT, EntraMembershipSyncStatus.TO_BE_DELETED),
                MembershipStatus.ACTIVE, EntraMembershipSyncStatus.SENT,
                MembershipStatus.INACTIVE, EntraMembershipSyncStatus.DELETION_SENT,
                EntraMembershipSyncStatus.NEEDS_REPUBLISH, responseCutoff, failedCutoff,
                0L, PageRequest.of(0, 500)).stream().map(UserEntraMembership::getId).toList();

        assertThat(selectedIds).containsExactly(oldAddition.getId(), oldDeletion.getId(),
                oldFailure.getId(), oldInactiveFailure.getId(), unsent.getId());
    }

    private UserEntraMembership membership(MembershipStatus membershipStatus, EntraMembershipSyncStatus entraStatus) {
        return UserEntraMembership.builder()
                .userEntraId(UUID.randomUUID())
                .resourceEntraId(UUID.randomUUID())
                .membershipStatus(membershipStatus)
                .entraStatus(entraStatus)
                .build();
    }
}
