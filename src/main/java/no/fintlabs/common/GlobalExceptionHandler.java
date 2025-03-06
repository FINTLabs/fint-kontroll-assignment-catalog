package no.fintlabs.common;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.slack.SlackMessenger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final SlackMessenger slackMessenger;

    public GlobalExceptionHandler(SlackMessenger slackMessenger) {
        this.slackMessenger = slackMessenger;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        String errorMessage = "⚠️ *Unhandled Exception!* \n" +
                              "🔹 *Message:* `" + (ex.getMessage() != null ? ex.getMessage() : ex.getCause().getMessage()) + "`\n" +
                              "🔹 *Time:* " + LocalDateTime.now() + "\n" +
                              "🔹 *Request URL:* `" + request.getDescription(false) + "`\n" +
                              "🔹 *Stack Trace*:\n ```" + getStackTrace(ex) + "```";

        slackMessenger.sendErrorMessage(errorMessage);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Request resulted in ResponseStatusException: {}", ex.getMessage());

        return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
    }

    private static String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString().length() > 3000 ? sb.substring(0, 3000) + "... (truncated)" : sb.toString();
    }
}
