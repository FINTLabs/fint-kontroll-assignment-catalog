package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssigmentEntityProducerServiceTest {
    @Mock
    private EntityProducerFactory entityProducerFactory;

    @Mock
    private EntityTopicService entityTopicService;

    @Mock
    private EntityProducer entityProducer;

    private AssigmentEntityProducerService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(entityProducerFactory.createProducer(any())).thenReturn(entityProducer);
        service = new AssigmentEntityProducerService(entityProducerFactory, entityTopicService);
    }

    @Test
    public void shouldPublish_validAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setIdentityProviderGroupObjectId(UUID.randomUUID());
        assignment.setIdentityProviderUserObjectId(UUID.randomUUID());

        service.publish(assignment);

        verify(entityProducer, times(1)).send(any(EntityProducerRecord.class));
    }

    @Test
    public void shouldNotPublish_inValidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publish(assignment);

        verify(entityProducer, times(0)).send(any(EntityProducerRecord.class));
    }

    @Test
    public void shouldPublishDeletion_validAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setIdentityProviderGroupObjectId(UUID.randomUUID());
        assignment.setIdentityProviderUserObjectId(UUID.randomUUID());

        service.publishDeletion(assignment);

        verify(entityProducer, times(1)).send(any(EntityProducerRecord.class));
    }

    @Test
    public void shouldNotPublishDeletion_invalidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publishDeletion(assignment);

        verify(entityProducer, times(0)).send(any(EntityProducerRecord.class));
    }
}
