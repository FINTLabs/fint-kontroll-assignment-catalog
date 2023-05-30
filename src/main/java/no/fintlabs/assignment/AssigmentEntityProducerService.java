package no.fintlabs.assignment;

import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import org.springframework.stereotype.Service;

@Service
public class AssigmentEntityProducerService {
    private final EntityProducer<SimpleAssignment> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;
    public AssigmentEntityProducerService( EntityProducerFactory entityProducerFactory)
    {
        entityProducer = entityProducerFactory.createProducer(SimpleAssignment.class);
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resource-group-membership")
                .build();
    }

    public void publish(Assignment assignment) {
        String key = assignment.getAssignmentId();
        entityProducer.send(
                EntityProducerRecord.<SimpleAssignment>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(key)
                        .value(assignment.toSimpleAssignment())
                        .build()
        );
    }
}
