package no.fintlabs.slack;

public record SlackMessage(String channel, String username, String icon_emoji, String text) {
}
