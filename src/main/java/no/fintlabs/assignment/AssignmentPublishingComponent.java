package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.user.entra.UserEntraMembershipRepublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class AssignmentPublishingComponent {
    private final UserEntraMembershipRepublisher membershipRepublisher;
    private final long responseTimeoutHours;

    public AssignmentPublishingComponent(
            UserEntraMembershipRepublisher membershipRepublisher,
            @Value("${fint.kontroll.assignment-catalog.reconciliation.response-timeout:72}") long responseTimeoutHours
    ) {
        this.membershipRepublisher = membershipRepublisher;
        this.responseTimeoutHours = responseTimeoutHours;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.reconciliation.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.reconciliation.fixed-delay}",
            timeUnit = TimeUnit.HOURS
    )
    public void republishStaleUserMemberships() {
        java.time.Instant now = java.time.Instant.now();
        Date responseCutoff = Date.from(now.minus(Duration.ofHours(responseTimeoutHours)));
        Date failedCutoff = Date.from(now.minus(Duration.ofHours(24)));
        log.info("Republishing user Entra memberships without a response since {} or with a failure before {}", responseCutoff, failedCutoff);

        membershipRepublisher.retryStaleMemberships(responseCutoff, failedCutoff);
    }
}
