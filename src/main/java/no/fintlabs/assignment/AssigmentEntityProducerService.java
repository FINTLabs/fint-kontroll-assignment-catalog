package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.exception.AssignmentMissingAzureGroupIdException;
import no.fintlabs.assignment.exception.AssignmentMissingAzureUserIdException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.azureadgroupmembership.AzureAdGroupMembership;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerFactory;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.topic.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.topic.EntityTopicService;
import no.fintlabs.membership.Membership;
import no.fintlabs.membership.MembershipService;
import org.springframework.stereotype.Service;

import java.util.List;
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
    ) {
        entityProducer = entityProducerFactory.createProducer(AzureAdGroupMembership.class);
        this.membershipService = membershipService;
        entityTopicNameParameters = EntityTopicNameParameters
                .builder()
                .resource("resource-group-membership")
                .build();
        entityTopicService.ensureTopic(entityTopicNameParameters, 0);
    }

    public void rePublish(List<Assignment> assignments) {
        if (!assignments.isEmpty()) {
            assignments
                    .stream()
                    .peek(assignment -> {
                        log.info("Republisering av assignment {} startet", assignment.getAssignmentId());
                    })
                    .forEach(assignment -> {
                        try {
                            publish(assignment);
                        } catch (Exception exception) {
                            log.error("Republisering av assignment {} feilet: {}"
                                    , assignment.getAssignmentId()
                                    , exception.getMessage()
                            );
                        }
                    });
        }
    }

    public void publish(FlattenedAssignment flattenedAssignment) {

        if (flattenedAssignment.getResourceRef() == null) {
            log.warn("Publishing flattened assignment {}. ResourceRef is null", flattenedAssignment.getId());
        }

        if (flattenedAssignment.getIdentityProviderGroupObjectId() == null ||
            flattenedAssignment.getIdentityProviderUserObjectId() == null) {
            log.warn(
                    "Publishing flattened assignment skipped for assignmentid {}. ResourceRef {}. Missing azuread group id ({}) or user " +
                    "id ({})",
                    flattenedAssignment.getId(),
                    flattenedAssignment.getResourceRef(),
                    flattenedAssignment.getIdentityProviderGroupObjectId(),
                    flattenedAssignment.getIdentityProviderUserObjectId()
            );
            return;
        }

        log.info("Publishing flattened assignment with id {}. Azuread groupId {}, azuread userId {}, resourceRef {}",
                 flattenedAssignment.getId(),
                 flattenedAssignment.getIdentityProviderGroupObjectId(),
                 flattenedAssignment.getIdentityProviderUserObjectId(),
                 flattenedAssignment.getResourceRef());

        publish(flattenedAssignment.getIdentityProviderGroupObjectId(), flattenedAssignment.getIdentityProviderUserObjectId());
    }

    public void publish(Assignment assignment) {

        if (assignment.getAzureAdGroupId() == null) {
            throw new AssignmentMissingAzureGroupIdException(assignment.getId(), assignment.getResourceRef());
        }
        log.info("Publisering: Azure groupId {} tilknyttet ressurs {} er funnet"
                , assignment.getAzureAdGroupId()
                , assignment.getResourceRef());

        if (assignment.getUserRef() != null) {
            if (assignment.getAzureAdUserId() == null) {
                throw new AssignmentMissingAzureUserIdException(assignment.getId(), assignment.getUserRef());
            }
            log.info("Publiserer brukertildeling " + assignment.getAssignmentId());

            publish(assignment.getAzureAdGroupId(), assignment.getAzureAdUserId());
        }
        if (assignment.getRoleRef() != null) {
            log.info("Publiserer gruppetildeling " + assignment.getAssignmentId());

            List<Membership> memberships =
                    membershipService.getMembersAssignedToRole(MembershipSpecificationBuilder.hasRoleId(assignment.getRoleRef()));

            if (memberships.isEmpty()) {
                log.info("Rolle {} mangler medlemmer", assignment.getRoleRef());
            } else {
                memberships
                        .stream()
                        .peek(membership -> {
                            log.info("Publisering av medlemskap {}", membership.getId())
                            ;
                        })
                        .peek(membership -> {
                            if (membership.getIdentityProviderUserObjectId() == null) {
                                log.info("Medlemskap {} mangler azure user object id", membership.getId());
                            }
                            ;
                        })
                        .map(Membership::getIdentityProviderUserObjectId)
                        .filter(azureUserId -> !(azureUserId == null))
                        .forEach(azureUserId -> publish(assignment.getAzureAdGroupId(), azureUserId));
            }
        }
    }

    public void publishDeletion(Assignment assignment) {
        if (assignment.getUserRef() != null) {
            publishDeletion(assignment.getAzureAdGroupId(), assignment.getAzureAdUserId());
        }
        if (assignment.getRoleRef() != null) {
            membershipService.getMembersAssignedToRole(MembershipSpecificationBuilder.hasRoleId(assignment.getRoleRef()))
                    .stream()
                    .map(Membership::getIdentityProviderUserObjectId)
                    .forEach(azureUserId -> publishDeletion(assignment.getAzureAdGroupId(), azureUserId));
        }
    }

    private void publishDeletion(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        entityProducer.send(
                EntityProducerRecord.<AzureAdGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(key)
                        .value(null)
                        .build()
        );
    }

    private void publish(UUID azureAdGroupId, UUID azureUserId) {
        String key = azureAdGroupId.toString() + "_" + azureUserId.toString();
        AzureAdGroupMembership azureAdGroupMembership = new AzureAdGroupMembership(key, azureAdGroupId, azureUserId);

        log.info("Publiserer ressurs " + azureAdGroupId + " tildelt bruker " + azureUserId);

        entityProducer.send(
                EntityProducerRecord.<AzureAdGroupMembership>builder()
                        .topicNameParameters(entityTopicNameParameters)
                        .key(key)
                        .value(azureAdGroupMembership)
                        .build()
        );
    }
}
