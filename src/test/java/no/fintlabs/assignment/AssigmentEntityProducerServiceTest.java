package no.fintlabs.assignment;

import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.groupmembership.ResourceGroupMembership;
import no.fintlabs.kafka.producing.ParameterizedTemplate;
import no.fintlabs.kafka.producing.ParameterizedTemplateFactory;
import no.fintlabs.kafka.topic.EntityTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssigmentEntityProducerServiceTest {
    @Mock
    private ParameterizedTemplateFactory parameterizedTemplateFactory;

    @Mock
    private EntityTopicService entityTopicService;

    @Mock
    private ParameterizedTemplate<ResourceGroupMembership> resourceGroupMembershipParameterizedTemplate;

    private AssigmentEntityProducerService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(parameterizedTemplateFactory.createTemplate(ResourceGroupMembership.class)).thenReturn(resourceGroupMembershipParameterizedTemplate);
        service = new AssigmentEntityProducerService(parameterizedTemplateFactory, entityTopicService);
    }

    @Test
    public void shouldPublish_validAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setIdentityProviderGroupObjectId(UUID.randomUUID());
        assignment.setIdentityProviderUserObjectId(UUID.randomUUID());

        service.publish(assignment);

        verify(resourceGroupMembershipParameterizedTemplate, times(1)).send(ArgumentMatchers.any());
    }

    /*@Test
    public void shouldNotPublish_inValidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publish(assignment);

        verify(resourceGroupMembershipParameterizedTemplate, times(0)).send(any(EntityProducerRecord.class));
    }

    @Test
    public void shouldPublishDeletion_validAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();
        assignment.setIdentityProviderGroupObjectId(UUID.randomUUID());
        assignment.setIdentityProviderUserObjectId(UUID.randomUUID());

        service.publishDeletion(assignment);

        verify(resourceGroupMembershipParameterizedTemplate, times(1)).send(any(EntityProducerRecord.class));
    }

    @Test
    public void shouldNotPublishDeletion_invalidAssignment() {
        FlattenedAssignment assignment = new FlattenedAssignment();

        service.publishDeletion(assignment);

        verify(resourceGroupMembershipParameterizedTemplate, times(0)).send(any(EntityProducerRecord.class));
    }*/
}
