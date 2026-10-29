package no.fintlabs.common;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import no.fintlabs.slack.SlackErrorNotifier;
import no.fintlabs.slack.SlackMessenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HttpErrorNotificationFilterTest {

    private SlackMessenger slackMessenger;
    private HttpErrorNotificationFilter filter;

    @BeforeEach
    void setUp() {
        slackMessenger = mock(SlackMessenger.class);
        SlackErrorNotifier notifier = new SlackErrorNotifier(
                slackMessenger, "https://grafana.example/explore", Set.of(429));
        filter = new HttpErrorNotificationFilter(notifier);
    }

    @Test
    void shouldSendAnActionableHttpErrorToSlackWithGrafanaLink() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, httpResponse) ->
                ((HttpServletResponse) httpResponse).setStatus(503));

        verify(slackMessenger).sendErrorMessage(contains("Status:* `503`"));
        verify(slackMessenger).sendErrorMessage(argThat(message ->
                message.contains("https://grafana.example/explore?from=")
                        && message.contains("&to=")
                        && !message.contains("from=now-3h")));
    }

    @Test
    void shouldNotSendExpectedNotFoundResponseToSlack() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assignments/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, httpResponse) ->
                ((HttpServletResponse) httpResponse).setStatus(404));

        verify(slackMessenger, never()).sendErrorMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldNotifyForConfiguredAdditionalStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, httpResponse) ->
                ((HttpServletResponse) httpResponse).setStatus(429));

        verify(slackMessenger).sendErrorMessage(contains("Status:* `429`"));
    }

    @Test
    void shouldSendUnhandledFilterExceptionAsHttp500() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("downstream failure");
                }))
                .isInstanceOf(ServletException.class);

        verify(slackMessenger).sendErrorMessage(contains("Status:* `500`"));
        verify(slackMessenger).sendErrorMessage(contains("ServletException: downstream failure"));
    }
}
