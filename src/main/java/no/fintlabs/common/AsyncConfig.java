package no.fintlabs.common;

import no.fintlabs.slack.SlackMessenger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private final SlackMessenger slackMessenger;

    public AsyncConfig(SlackMessenger slackMessenger) {
        this.slackMessenger = slackMessenger;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new GlobalAsyncExceptionHandler(slackMessenger);
    }
}
