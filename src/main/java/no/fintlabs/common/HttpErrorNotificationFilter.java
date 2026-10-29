package no.fintlabs.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.slack.SlackErrorNotifier;
import no.fintlabs.slack.SlackNotificationMarkers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reports only actionable HTTP failures. 404s and ordinary validation errors
 * are deliberately left out unless their status is explicitly configured.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "fint.kontroll.slack.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpErrorNotificationFilter extends OncePerRequestFilter {

    public static final String HANDLED_EXCEPTION_ATTRIBUTE =
            HttpErrorNotificationFilter.class.getName() + ".HANDLED_EXCEPTION";
    private static final String NOTIFIED_ATTRIBUTE =
            HttpErrorNotificationFilter.class.getName() + ".NOTIFIED";

    private final SlackErrorNotifier slackErrorNotifier;

    public HttpErrorNotificationFilter(SlackErrorNotifier slackErrorNotifier) {
        this.slackErrorNotifier = slackErrorNotifier;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            notifyIfNeeded(request, 500, exception);
            throw exception;
        }

        notifyIfNeeded(request, response.getStatus(),
                (Throwable) request.getAttribute(HANDLED_EXCEPTION_ATTRIBUTE));
    }

    private void notifyIfNeeded(HttpServletRequest request, int status, Throwable exception) {
        if (Boolean.TRUE.equals(request.getAttribute(NOTIFIED_ATTRIBUTE))
                || !slackErrorNotifier.shouldNotifyForHttpStatus(status)) {
            return;
        }

        request.setAttribute(NOTIFIED_ATTRIBUTE, true);

        if (exception != null) {
            log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                    "Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
        } else {
            log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                    "Request {} {} completed with HTTP {}", request.getMethod(), request.getRequestURI(), status);
        }

        slackErrorNotifier.notifyHttpError(status, request, exception);
    }
}
