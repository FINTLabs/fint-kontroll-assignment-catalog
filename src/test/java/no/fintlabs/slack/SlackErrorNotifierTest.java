package no.fintlabs.slack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SlackErrorNotifierTest {

    @Test
    void shouldLinkToAGrafanaWindowAroundTheLogTimestamp() {
        SlackMessenger slackMessenger = mock(SlackMessenger.class);
        SlackErrorNotifier notifier = new SlackErrorNotifier(slackMessenger,
                "https://grafana.flais.io/d/dashboard/logs?orgId=1&from=now-3h&to=now", Set.of(429));
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Failure");
        event.setTimeStamp(1_700_000_000_000L);

        notifier.notifyErrorLog(event);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(slackMessenger).sendErrorMessage(message.capture());
        assertThat(message.getValue()).contains("from=1699999700000&to=1700000300000");
        assertThat(message.getValue()).doesNotContain("from=now-3h");
    }
}
