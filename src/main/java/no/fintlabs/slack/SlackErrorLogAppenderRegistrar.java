package no.fintlabs.slack;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "fint.kontroll.slack.enabled", havingValue = "true")
public class SlackErrorLogAppenderRegistrar {

    private final SlackErrorNotifier slackErrorNotifier;

    public SlackErrorLogAppenderRegistrar(SlackErrorNotifier slackErrorNotifier) {
        this.slackErrorNotifier = slackErrorNotifier;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAppender() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext)) {
            return;
        }

        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(SlackErrorLogAppender.NAME) != null) {
            return;
        }

        SlackErrorLogAppender appender = new SlackErrorLogAppender(slackErrorNotifier);
        appender.setContext(loggerContext);
        appender.start();
        rootLogger.addAppender(appender);
    }
}
