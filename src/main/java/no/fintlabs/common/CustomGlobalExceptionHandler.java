package no.fintlabs.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ProblemDetailFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import no.fintlabs.slack.SlackNotificationMarkers;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomGlobalExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    public CustomGlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = problemDetailFactory.createProblemDetail(ex, request);
        request.setAttribute(HttpErrorNotificationFilter.HANDLED_EXCEPTION_ATTRIBUTE, ex);
        Object correlationId = problem.getProperties() == null ? null : problem.getProperties().get("correlationId");
        request.setAttribute(HttpErrorNotificationFilter.CORRELATION_ID_ATTRIBUTE, correlationId);

        if (problem.getStatus() >= 500) {
            log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                    "HTTP {} {} {} correlationId={} exception={} detail={}",
                    problem.getStatus(), request.getMethod(), request.getRequestURI(), correlationId,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
        } else {
            log.warn("HTTP {} {} {} correlationId={} exception={} detail={}",
                    problem.getStatus(), request.getMethod(), request.getRequestURI(), correlationId,
                    ex.getClass().getSimpleName(), ex.getMessage());
        }

        return problem;
    }
}
