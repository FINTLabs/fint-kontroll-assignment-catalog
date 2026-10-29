package no.fintlabs.assignment;

import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssignmentPublishingComponentTest {

    @Test
    void republishStaleUserMemberships_usesTheConfiguredResponseGracePeriod() {
        FlattenedAssignmentService flattenedAssignmentService = mock(FlattenedAssignmentService.class);
        AssignmentPublishingComponent component = new AssignmentPublishingComponent(flattenedAssignmentService, 72);
        long before = System.currentTimeMillis() - 72L * 60 * 60 * 1000;

        component.republishStaleUserMemberships();

        verify(flattenedAssignmentService).republishStaleUserEntraMemberships(
                argThat(cutoff -> isWithinOneSecondOf(cutoff, before)));
    }

    private boolean isWithinOneSecondOf(Date value, long expectedMillis) {
        return Math.abs(value.getTime() - expectedMillis) < 1_000;
    }
}
