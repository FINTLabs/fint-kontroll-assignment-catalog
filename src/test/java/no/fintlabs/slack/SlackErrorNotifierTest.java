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
                "https://grafana.flais.io/d/cg2oxl949bpq8d/kontroll-logs?from=now-12h&to=now&timezone=browser&var-filters=old",
                "fint-kontroll-assignment-catalog", "fintlabs-no", Set.of(429));
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.ERROR);
        event.setLoggerName("no.fintlabs.Example");
        event.setMessage("Failure");
        event.setTimeStamp(1_700_000_000_000L);

        notifier.notifyErrorLog(event);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(slackMessenger).sendErrorMessage(message.capture());
        assertThat(message.getValue())
                .contains("/kontroll-logs?timezone=browser&from=1699999970000&to=1700000030000")
                .contains("var-filters=app%7C%3D%7Cfint-kontroll-assignment-catalog")
                .contains("var-filters=namespace%7C%3D%7Cfintlabs-no")
                .doesNotContain("now-12h", "var-filters=old");
    }
}
