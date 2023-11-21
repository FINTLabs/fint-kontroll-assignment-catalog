package no.fintlabs.assignment;

import no.fintlabs.azureAdGroupMembership.AzureAdGroupMembership;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.membership.MembershipSpecificationBuilder;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

@Service
public class AssigmentEntityProducerService {
    private final EntityProducer<AzureAdGroupMembership> entityProducer;
    private final EntityTopicNameParameters entityTopicNameParameters;
    private final MembershipService membershipService;
    public AssigmentEntityProducerService(
            EntityProducerFactory entityProducerFactory,
            EntityTopicService entityTopicService,
            MembershipService membershipService
    ){
        entityProducer = entityProducerFactory.createProducer(AzureAdGroupMembership.class);
        this.membershipService = membershipService;
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resource-group-membership")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters, 0);
    }

    public void publish(Assignment assignment) {

        if (assignment.getUserRef() != null) {
            publish(assignment.getAzureAdGroupId(), assignment.getAzureAdUserId());
        }
        if (assignment.getRoleRef() != null) {
            membershipService.getMembersAssignedToRole(roleEquals(assignment.getRoleRef()))
                    .stream()
                    .map(Membership::getIdentityProviderUserObjectId)
                    .forEach(azureUserId -> publish(assignment.getAzureAdGroupId(),azureUserId ));            ;
        }
    }
    private void publish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        AzureAdGroupMembership azureAdGroupMembership = new AzureAdGroupMembership(key, azureAdGroupId, azureUserId);
        entityProducer.send(
                EntityProducerRecord.<AzureAdGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }
    private  Specification<Membership> roleEquals(Long roleId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("roleId"), roleId);
    }
}
