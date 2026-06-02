package no.fintlabs.kafka;

import no.novari.kafka.consuming.ListenerConfiguration;
import no.novari.kafka.topic.configuration.EventCleanupFrequency;
import no.novari.kafka.topic.configuration.EventTopicConfiguration;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;

import java.time.Duration;

public final class KafkaEventTopics {

    private static final int DEFAULT_PARTITIONS = 1;

    private KafkaEventTopics() {
    }

    public static EventTopicNameParameters topicNameParameters(String eventName) {
        return EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build())
                .eventName(eventName)
                .build();
    }

    public static EventTopicConfiguration topicConfiguration() {
        return EventTopicConfiguration
                .stepBuilder()
                .partitions(DEFAULT_PARTITIONS)
                .retentionTime(Duration.ofDays(7))
                .cleanupFrequency(EventCleanupFrequency.NORMAL)
                .build();
    }

    public static ListenerConfiguration defaultListenerConfiguration() {
        return ListenerConfiguration
                .stepBuilder()
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .continueFromPreviousOffsetOnAssignment()
                .build();
    }
}
