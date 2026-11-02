package no.fintlabs.slack;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class SlackMessengerTest {

    @Test
    void shouldPostErrorMessagesUsingSlackWebApiAndBotToken() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SlackMessenger messenger = new SlackMessenger(restTemplate);
        ReflectionTestUtils.setField(messenger, "slackEnabled", true);
        ReflectionTestUtils.setField(messenger, "slackApiUrl", "https://slack.com/api/chat.postMessage");
        ReflectionTestUtils.setField(messenger, "botToken", "xoxb-test-token");
        ReflectionTestUtils.setField(messenger, "errorChannelId", "C12345678");
        ReflectionTestUtils.setField(messenger, "authorizedOrgId", "fintlabs.no");
        ReflectionTestUtils.setField(messenger, "app", "fint-kontroll-assignment-catalog");

        server.expect(requestTo("https://slack.com/api/chat.postMessage"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer xoxb-test-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json("""
                        {"channel":"C12345678","text":"🔺 fintlabs.no - fint-kontroll-assignment-catalog🔺\\n\\nAlert"}
                        """))
                .andRespond(withSuccess("{" + "\"ok\":true" + "}", MediaType.APPLICATION_JSON));

        assertThat(messenger.sendErrorMessage("Alert")).isTrue();
        server.verify();
    }
}
