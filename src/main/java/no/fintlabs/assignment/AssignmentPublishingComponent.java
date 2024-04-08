package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AssignmentPublishingComponent {
    private final FlattenedAssignmentService flattenedAssignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public AssignmentPublishingComponent(FlattenedAssignmentService flattenedAssignmentService, AssigmentEntityProducerService assigmentEntityProducerService) {
        this.flattenedAssignmentService = flattenedAssignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.flattenedpublishing.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.flattenedpublishing.fixed-delay}",
            timeUnit = TimeUnit.MINUTES
    )
    public void publishFlattenedAssignmentsUnConfirmed() {
        log.info("Publishing unconfirmed flattened assignments");

        flattenedAssignmentService.getAllFlattenedAssignments()
                .stream().filter(flattenedAssignment -> !flattenedAssignment.isIdentityProviderGroupMembershipConfirmed())
                .forEach(assigmentEntityProducerService::publish);
    }
}
