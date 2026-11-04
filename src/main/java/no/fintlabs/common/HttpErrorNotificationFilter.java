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
    public static final String CORRELATION_ID_ATTRIBUTE =
            HttpErrorNotificationFilter.class.getName() + ".CORRELATION_ID";
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
            log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                    "HTTP 500 {} {} exception={} detail={}", request.getMethod(), request.getRequestURI(),
                    exception.getClass().getSimpleName(), exception.getMessage(), exception);
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

        if (exception == null) {
            if (status >= 500) {
                log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                        "HTTP {} {} {}", status, request.getMethod(), request.getRequestURI());
            } else {
                log.warn("HTTP {} {} {}", status, request.getMethod(), request.getRequestURI());
            }
        }

        slackErrorNotifier.notifyHttpError(status, request, exception);
    }
}
