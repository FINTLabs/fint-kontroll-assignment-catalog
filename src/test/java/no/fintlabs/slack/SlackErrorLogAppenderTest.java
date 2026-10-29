package no.fintlabs.slack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SlackErrorLogAppenderTest {

    @Test
    void shouldForwardOrdinaryErrorLogs() {
        SlackErrorNotifier notifier = mock(SlackErrorNotifier.class);
        SlackErrorLogAppender appender = new SlackErrorLogAppender(notifier);
        appender.start();
        LoggingEvent event = event(Level.ERROR);

        appender.doAppend(event);

        verify(notifier).notifyErrorLog(event);
    }

    @Test
    void shouldNotDuplicateDirectNotifications() {
        SlackErrorNotifier notifier = mock(SlackErrorNotifier.class);
        SlackErrorLogAppender appender = new SlackErrorLogAppender(notifier);
        appender.start();
        LoggingEvent event = event(Level.ERROR);
        event.addMarker(SlackNotificationMarkers.NOTIFIED_DIRECTLY);

        appender.doAppend(event);

        verify(notifier, never()).notifyErrorLog(event);
    }

    private static LoggingEvent event(Level level) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(level);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Something failed");
        return event;
    }
}
