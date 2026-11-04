package no.fintlabs.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.slack.SlackErrorNotifier;
import no.fintlabs.slack.SlackMessenger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomGlobalExceptionHandlerTest {

    @Test
    void missingRoleProducesOneConciseLogAndCorrelatedSlackAlert() throws Exception {
        ProblemDetailFactory factory = mock(ProblemDetailFactory.class);
        SlackMessenger messenger = mock(SlackMessenger.class);
        HttpErrorNotificationFilter filter = new HttpErrorNotificationFilter(new SlackErrorNotifier(
                messenger, "https://grafana.example/logs", "fint-kontroll-assignment-catalog", "fintlabs-no", Set.of(404)));
        ResourceNotFoundException exception = new ResourceNotFoundException("Role not found: 6999999996");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setProperty("correlationId", "trace-123");
        when(factory.createProblemDetail(exception, request)).thenReturn(problem);

        Logger handlerLogger = (Logger) LoggerFactory.getLogger(CustomGlobalExceptionHandler.class);
        Logger filterLogger = (Logger) LoggerFactory.getLogger(HttpErrorNotificationFilter.class);
        ListAppender<ILoggingEvent> handlerLogs = new ListAppender<>();
        ListAppender<ILoggingEvent> filterLogs = new ListAppender<>();
        handlerLogs.start();
        filterLogs.start();
        handlerLogger.addAppender(handlerLogs);
        filterLogger.addAppender(filterLogs);
        try {
            filter.doFilter(request, response, (ignoredRequest, httpResponse) -> {
                new CustomGlobalExceptionHandler(factory).handleGenericException(exception, request);
                ((MockHttpServletResponse) httpResponse).setStatus(404);
            });

            assertThat(handlerLogs.list).hasSize(1);
            assertThat(handlerLogs.list.getFirst().getLevel()).isEqualTo(Level.WARN);
            assertThat(handlerLogs.list.getFirst().getFormattedMessage())
                    .contains("HTTP 404 POST /api/assignments correlationId=trace-123")
                    .contains("Role not found: 6999999996");
            assertThat(handlerLogs.list.getFirst().getThrowableProxy()).isNull();
            assertThat(filterLogs.list).isEmpty();
            verify(messenger).sendErrorMessage(contains("Correlation ID:* `trace-123`"));
            verify(messenger).sendErrorMessage(argThat(message -> message.contains("var-lineContains=trace-123")));
        } finally {
            handlerLogger.detachAppender(handlerLogs);
            filterLogger.detachAppender(filterLogs);
        }
    }
}
