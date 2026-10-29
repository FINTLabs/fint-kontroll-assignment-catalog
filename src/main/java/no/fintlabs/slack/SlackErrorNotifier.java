package no.fintlabs.slack;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "fint.kontroll.slack.enabled", havingValue = "true")
public class SlackErrorNotifier {

    private static final int MAX_STACK_TRACE_LENGTH = 3_000;
    private static final Duration GRAFANA_CONTEXT_WINDOW = Duration.ofMinutes(5);

    private final SlackMessenger slackMessenger;
    private final String grafanaUrl;
    private final Set<Integer> additionalHttpStatuses;

    public SlackErrorNotifier(
            SlackMessenger slackMessenger,
            @Value("${fint.kontroll.slack.grafana-url:}") String grafanaUrl,
            @Value("${fint.kontroll.slack.additional-http-statuses:429}") Set<Integer> additionalHttpStatuses) {
        this.slackMessenger = slackMessenger;
        this.grafanaUrl = grafanaUrl;
        this.additionalHttpStatuses = additionalHttpStatuses;
    }

    public boolean shouldNotifyForHttpStatus(int status) {
        return status >= 500 || additionalHttpStatuses.contains(status);
    }

    public void notifyHttpError(int status, HttpServletRequest request, Throwable exception) {
        StringBuilder message = new StringBuilder()
                .append("⚠️ *HTTP error response*\n")
                .append("🔹 *Status:* `").append(status).append("`\n")
                .append("🔹 *Request:* `").append(request.getMethod()).append(" ")
                .append(request.getRequestURI()).append("`");

        if (exception != null) {
            message.append("\n🔹 *Exception:* `")
                    .append(exception.getClass().getSimpleName())
                    .append(": ").append(safeMessage(exception))
                    .append("`");
        }

        dispatch(message, Instant.now());
    }

    public void notifyErrorLog(ILoggingEvent event) {
        StringBuilder message = new StringBuilder()
                .append("⚠️ *Error log detected*\n")
                .append("🔹 *Logger:* `").append(event.getLoggerName()).append("`\n")
                .append("🔹 *Message:* ").append(event.getFormattedMessage());

        if (event.getThrowableProxy() != null) {
            message.append("\n🔹 *Stack trace:*\n```")
                    .append(truncate(ThrowableProxyUtil.asString(event.getThrowableProxy())))
                    .append("```");
        }

        dispatch(message, Instant.ofEpochMilli(event.getTimeStamp()));
    }

    private void dispatch(StringBuilder message, Instant occurredAt) {
        appendGrafanaLink(message, occurredAt);
        slackMessenger.sendErrorMessage(message.toString());
    }

    private void appendGrafanaLink(StringBuilder message, Instant occurredAt) {
        if (!grafanaUrl.isBlank()) {
            message.append("\n🔹 *Investigate:* <")
                    .append(grafanaLinkFor(occurredAt))
                    .append("|Open Grafana logs around this error>");
        }
    }

    private String grafanaLinkFor(Instant occurredAt) {
        try {
            return UriComponentsBuilder.fromUriString(grafanaUrl)
                    .replaceQueryParam("from", occurredAt.minus(GRAFANA_CONTEXT_WINDOW).toEpochMilli())
                    .replaceQueryParam("to", occurredAt.plus(GRAFANA_CONTEXT_WINDOW).toEpochMilli())
                    .build()
                    .encode()
                    .toUriString();
        } catch (IllegalArgumentException exception) {
            return grafanaUrl;
        }
    }

    private static String safeMessage(Throwable exception) {
        return exception.getMessage() == null ? "No exception message" : exception.getMessage();
    }

    private static String truncate(String value) {
        return value.length() <= MAX_STACK_TRACE_LENGTH
                ? value
                : value.substring(0, MAX_STACK_TRACE_LENGTH) + "... (truncated)";
    }
}
