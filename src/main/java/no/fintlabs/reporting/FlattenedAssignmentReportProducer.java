package no.fintlabs.reporting;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlattenedAssignmentReportProducer {
    private final EntityProducer<FlattenedAssignmentReport> entityProducer;
    private final EntityTopicNameParameters topicNameParameters;

    public FlattenedAssignmentReportProducer(EntityProducerFactory entityProducerFactory, EntityTopicService entityTopicService) {
        entityProducer = entityProducerFactory.createProducer(FlattenedAssignmentReport.class);

        topicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("flattened-assignment-reporting")
                .build();
        entityTopicService.ensureTopic(topicNameParameters, 0);
    }

    public void publish(FlattenedAssignmentReport flattenedAssignmentReport) {
        String key = flattenedAssignmentReport.id().toString();

        entityProducer.send(
                EntityProducerRecord.<FlattenedAssignmentReport>builder()
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
