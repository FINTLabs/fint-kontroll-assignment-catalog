package no.fintlabs.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SlackMessenger {

    @Value("${fint.kontroll.slack.api-url:https://slack.com/api/chat.postMessage}")
    private String slackApiUrl;

    @Value("${fint.kontroll.slack.bot-token:}")
    private String botToken;

    @Value("${fint.kontroll.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${fint.kontroll.authorization.authorized-org-id:vigo.no}")
    private String authorizedOrgId;

    @Value("${fint.application-id:app}")
    private String app;

    @Value("${fint.kontroll.slack.channel-id:fint-kontroll-errors}")
    private String errorChannelId;

    private final RestTemplate restTemplate;

    public SlackMessenger(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendErrorMessage(String message) {
        String msg = "🔺 " + authorizedOrgId + " - " + app + "🔺\n\n" + message;
        return sendMessage(new SlackMessage(errorChannelId, msg));
    }

    public boolean sendMessage(SlackMessage slackMessage) {
        log.debug("Sending Slack message; enabled={}", slackEnabled);

        if (slackEnabled) {
            if (botToken.isBlank()) {
                log.warn("Slack sending is enabled but no bot token is configured");
                return false;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(botToken);
                headers.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<SlackApiResponse> response = restTemplate.exchange(
                        slackApiUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(slackMessage, headers),
                        SlackApiResponse.class);

                SlackApiResponse responseBody = response.getBody();
                if (responseBody != null && responseBody.ok()) {
                    return true;
                }

                log.warn("Slack rejected the error notification: {}",
                        responseBody == null ? "empty response" : responseBody.error());
            } catch (Exception e) {
                log.warn("Unable to send Slack error notification", e);
            }
        } else {
            log.info("Slack sending disabled, slack message {}", slackMessage);
        }
        return false;
    }
}
