package no.fintlabs.common;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.slack.SlackMessenger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final SlackMessenger slackMessenger;

    public GlobalAsyncExceptionHandler(SlackMessenger slackMessenger) {
        this.slackMessenger = slackMessenger;
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Unhandled exception from an async method: {}", method.getName(), ex);

        slackMessenger.sendErrorMessage("⚠️ *Unhandled Exception!* \n" +
                                        "🔹 *Message:* `" + (ex.getMessage() != null ? ex.getMessage() : ex.getCause().getMessage()) + "`\n" +
                                        "🔹 *Time:* " + LocalDateTime.now() + "\n" +
                                        "🔹 *Method:* `" + method.getName() + "`\n" +
                                        "🔹 *Stack Trace*:\n ```" + getStackTrace(ex) + "```");
    }

    private static String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString().length() > 3000 ? sb.substring(0, 3000) + "... (truncated)" : sb.toString();
    }
}
