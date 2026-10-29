package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class AssignmentPublishingComponent {
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final long responseTimeoutHours;

    public AssignmentPublishingComponent(
            FlattenedAssignmentService flattenedAssignmentService,
            @Value("${fint.kontroll.assignment-catalog.reconciliation.response-timeout:72}") long responseTimeoutHours
    ) {
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.responseTimeoutHours = responseTimeoutHours;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.reconciliation.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.reconciliation.fixed-delay}",
            timeUnit = TimeUnit.HOURS
    )
    public void republishStaleUserMemberships() {
        Date cutoff = Date.from(java.time.Instant.now().minus(Duration.ofHours(responseTimeoutHours)));
        log.info("Republishing user Entra memberships without a response since {}", cutoff);

        flattenedAssignmentService.republishStaleUserEntraMemberships(cutoff);
    }
}
