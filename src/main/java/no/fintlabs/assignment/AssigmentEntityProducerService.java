package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.azureAdGroupMembership.AzureAdGroupMembership;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipService;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

@Slf4j
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

        if (assignment.getAzureAdGroupId() == null) {
            throw new AssignmentMissingAzureGroupIdException(assignment.getId(), assignment.getResourceRef());
        }
        log.info("Publiserng: Azure groupId " +assignment.getAzureAdGroupId() + " for ressurs er funnet");

        if (assignment.getUserRef() != null) {
            if (assignment.getAzureAdUserId() == null) {
                throw new AssignmentMissingAzureUserIdException(assignment.getId(), assignment.getUserRef());
            }
            log.info("Publiserer brukertildeling " + assignment.getAssignmentId());

            publish(assignment.getAzureAdGroupId(), assignment.getAzureAdUserId());
        }
        if (assignment.getRoleRef() != null) {
            log.info("Publiserer gruppetildeling " + assignment.getAssignmentId());

            membershipService.getMembersAssignedToRole(roleEquals(assignment.getRoleRef()))
                    .stream()
                    .map( membership -> {
                        return Optional.of(membership.getIdentityProviderUserObjectId());
                    })
                    .filter(azureUserId -> azureUserId.isPresent())
                    .map(azureUserId-> azureUserId.get())
                    .forEach(azureUserId -> publish(assignment.getAzureAdGroupId(),azureUserId ));            ;
        }
    }
    private void publish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        AzureAdGroupMembership azureAdGroupMembership = new AzureAdGroupMembership(key, azureAdGroupId, azureUserId);
        log.info("Ressurs" + azureAdGroupId + " tildelt bruker " + azureUserId);
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
