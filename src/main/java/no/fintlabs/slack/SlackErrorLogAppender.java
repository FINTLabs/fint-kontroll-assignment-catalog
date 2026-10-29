package no.fintlabs.slack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class SlackErrorLogAppender extends AppenderBase<ILoggingEvent> {

    public static final String NAME = "SLACK_ERROR_LOG_APPENDER";

    private final SlackErrorNotifier slackErrorNotifier;

    public SlackErrorLogAppender(SlackErrorNotifier slackErrorNotifier) {
        this.slackErrorNotifier = slackErrorNotifier;
        setName(NAME);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!event.getLevel().isGreaterOrEqual(Level.ERROR) || isDirectNotification(event)) {
            return;
        }

        slackErrorNotifier.notifyErrorLog(event);
    }

    private boolean isDirectNotification(ILoggingEvent event) {
        return event.getMarkerList() != null && event.getMarkerList().stream()
                .anyMatch(marker -> marker.contains(SlackNotificationMarkers.NOTIFIED_DIRECTLY));
    }
}
