package no.fintlabs.assignment;

import no.fintlabs.assignment.user.entra.UserEntraMembershipRepublisher;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssignmentPublishingComponentTest {

    @Test
    void republishStaleUserMemberships_usesTheConfiguredResponseGracePeriod() {
        UserEntraMembershipRepublisher membershipRepublisher = mock(UserEntraMembershipRepublisher.class);
        AssignmentPublishingComponent component = new AssignmentPublishingComponent(membershipRepublisher, 72);
        long before = System.currentTimeMillis() - 72L * 60 * 60 * 1000;

        component.republishStaleUserMemberships();

        verify(membershipRepublisher).retryStaleMemberships(
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before)),
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before + 48L * 60 * 60 * 1000)));
    }

    private boolean isWithinOneSecondOf(Date value, long expectedMillis) {
        return Math.abs(value.getTime() - expectedMillis) < 1_000;
    }
}
