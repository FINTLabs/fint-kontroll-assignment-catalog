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
        log.error(SlackNotificationMarkers.NOTIFIED_DIRECTLY,
                "Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        request.setAttribute(HttpErrorNotificationFilter.HANDLED_EXCEPTION_ATTRIBUTE, ex);

        return problemDetailFactory.createProblemDetail(ex, request);
    }
}
