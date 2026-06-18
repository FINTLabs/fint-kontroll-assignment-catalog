package no.fintlabs.kafka;

import no.novari.kafka.consuming.ListenerConfiguration;
import no.novari.kafka.topic.configuration.EventCleanupFrequency;
import no.novari.kafka.topic.configuration.EventTopicConfiguration;
import no.novari.kafka.topic.configuration.EntityCleanupFrequency;
import no.novari.kafka.topic.configuration.EntityTopicConfiguration;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import no.novari.kafka.topic.name.EventTopicNameParameters;
import no.novari.kafka.topic.name.TopicNamePrefixParameters;

import java.time.Duration;

public final class KafkaEntityTopics {

    private static final int DEFAULT_PARTITIONS = 1;
    private static final Duration DEFAULT_NULL_VALUE_RETENTION_TIME = Duration.ofDays(1);

    private KafkaEntityTopics() {
    }

    public static EntityTopicNameParameters topicNameParameters(String resourceName) {
        return EntityTopicNameParameters
                .builder()
                .topicNamePrefixParameters(defaultTopicNamePrefixParameters())
                .resourceName(resourceName)
                .build();
    }

    public static EventTopicNameParameters eventTopicNameParameters(String eventName) {
        return EventTopicNameParameters
                .builder()
                .topicNamePrefixParameters(defaultTopicNamePrefixParameters())
                .eventName(eventName)
                .build();
    }

    private static TopicNamePrefixParameters defaultTopicNamePrefixParameters() {
        return TopicNamePrefixParameters
                .stepBuilder()
                .orgIdApplicationDefault()
                .domainContextApplicationDefault()
                .build();
    }

    public static EntityTopicConfiguration compactedTopicConfiguration() {
        return compactedTopicConfiguration(null);
    }

    public static EntityTopicConfiguration compactedTopicConfiguration(Duration lastValueRetentionTime) {
        var lastValueRetentionStep = EntityTopicConfiguration
                .stepBuilder()
                .partitions(DEFAULT_PARTITIONS);

        var nullValueRetentionStep =
                lastValueRetentionTime == null || lastValueRetentionTime.isZero()
                        ? lastValueRetentionStep.lastValueRetainedForever()
                        : lastValueRetentionStep.lastValueRetentionTime(lastValueRetentionTime);

        return nullValueRetentionStep
                .nullValueRetentionTime(DEFAULT_NULL_VALUE_RETENTION_TIME)
                .cleanupFrequency(EntityCleanupFrequency.NORMAL)
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

    public static EventTopicConfiguration eventTopicConfiguration() {
        return EventTopicConfiguration
                .stepBuilder()
                .partitions(DEFAULT_PARTITIONS)
                .retentionTime(DEFAULT_NULL_VALUE_RETENTION_TIME)
                .cleanupFrequency(EventCleanupFrequency.NORMAL)
                .build();
    }
}
