package no.fintlabs.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.slack.SlackMessenger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomGlobalExceptionHandler {

    private final SlackMessenger slackMessenger;
    private final ProblemDetailFactory problemDetailFactory;

    public CustomGlobalExceptionHandler(SlackMessenger slackMessenger, ProblemDetailFactory problemDetailFactory) {
        this.slackMessenger = slackMessenger;
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        String errorMessage = "⚠️ *Unhandled Exception!* \n" +
                              "🔹 *Message:* `" + (ex.getMessage() != null ? ex.getMessage() : ex.getCause().getMessage()) + "`\n" +
                              "🔹 *Time:* " + LocalDateTime.now() + "\n" +
                              "🔹 *Request URL:* `" + request.getRequestURI() + "`\n" +
                              "🔹 *Stack Trace*:\n ```" + getStackTrace(ex) + "```";

        slackMessenger.sendErrorMessage(errorMessage);

        return problemDetailFactory.createProblemDetail(ex, request);
    }

    private static String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString().length() > 3000 ? sb.substring(0, 3000) + "... (truncated)" : sb.toString();
    }
}
