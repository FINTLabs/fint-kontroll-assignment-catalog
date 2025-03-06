package no.fintlabs.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SlackMessenger {

    @Value("${fint.kontroll.slack.url}")
    private String slackUrl;

    @Value("${fint.kontroll.slack.enabled}")
    private Boolean slackEnabled;

    @Value("${fint.kontroll.authorization.authorized-org-id:vigo.no}")
    private String authorizedOrgId;

    @Value("${fint.relations.default-base-url:localhost}")
    private String baseUrl;

    @Value("${fint.application-id:app}")
    private String app;

    private final RestTemplate restTemplate;

    public SlackMessenger(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendErrorMessage(String message) {
        String channel = "fint-kontroll-errors";
        String msg = "🔺 " + authorizedOrgId + " - " + app + "🔺\n\n" + message;
        return sendMessage(new SlackMessage(getChannel(channel), "SlackMessageBot", ":disguised_face:" , msg));
    }

    private boolean isBeta() {
        log.info("Environment is: {}", baseUrl);

        if (baseUrl.contains("beta")) {
            log.info("Auth: Is beta");
            return true;
        }

        return false;
    }

    private String getChannel(String channel) {
        return isBeta() ? "fint-kontroll-errors-beta" : channel;
    }

    public boolean sendMessage(SlackMessage slackMessage) {
        log.info("Sending Slack message {}", slackMessage.text());
        if (slackEnabled) {
            try {
                restTemplate.postForLocation(slackUrl, slackMessage);
                return true;
            } catch (Exception e) {
                log.warn("Unable to send Slack message " + slackMessage.text(), e);
            }
        } else {
            log.info("Slack sending disabled, slack message {}", slackMessage);
        }
        return false;
    }
}
