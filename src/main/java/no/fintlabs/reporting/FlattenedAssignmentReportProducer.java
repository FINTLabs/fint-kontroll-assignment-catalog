package no.fintlabs.reporting;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.KafkaEntityTopics;
import no.novari.kafka.producing.ParameterizedProducerRecord;
import no.novari.kafka.producing.ParameterizedTemplate;
import no.novari.kafka.producing.ParameterizedTemplateFactory;
import no.novari.kafka.topic.EntityTopicService;
import no.novari.kafka.topic.name.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlattenedAssignmentReportProducer {
    private final ParameterizedTemplate<FlattenedAssignmentReport> entityProducer;
    private final EntityTopicNameParameters topicNameParameters;

    public FlattenedAssignmentReportProducer(ParameterizedTemplateFactory entityProducerFactory, EntityTopicService entityTopicService) {
        entityProducer = entityProducerFactory.createTemplate(FlattenedAssignmentReport.class);

        topicNameParameters = KafkaEntityTopics.topicNameParameters("flattened-assignment-reporting");
        entityTopicService.createOrModifyTopic(topicNameParameters, KafkaEntityTopics.compactedTopicConfiguration());
    }

    public void publish(FlattenedAssignmentReport flattenedAssignmentReport) {
        String key = flattenedAssignmentReport.id().toString();

        entityProducer.send(
                ParameterizedProducerRecord.<FlattenedAssignmentReport>builder()
                        .topicNameParameters(topicNameParameters)
                        .key(key)
                        .value(flattenedAssignmentReport)
                        .build()
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish report for id: {}", key, ex);
            } else {
                log.info("Successfully published report for id: {}", key);
            }
        });
    }
}
