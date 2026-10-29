package no.fintlabs.slack;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Marks log events that have their own, richer Slack notification. The log
 * appender must not turn these into a second notification.
 */
public final class SlackNotificationMarkers {

    public static final Marker NOTIFIED_DIRECTLY = MarkerFactory.getMarker("SLACK_NOTIFIED_DIRECTLY");

    private SlackNotificationMarkers() {
    }
}
