package no.fintlabs.assignment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AssignmentRePublishingComponent {
    private final AssignmentService assignmentService;
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public AssignmentRePublishingComponent(AssignmentService assignmentService, AssigmentEntityProducerService assigmentEntityProducerService) {
        this.assignmentService = assignmentService;
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    @Scheduled(
            initialDelayString = "${fint.kontroll.assignment-catalog.publishing.initial-delay}",
            fixedDelayString = "${fint.kontroll.assignment-catalog.publishing.fixed-delay}"
    )
    public void republishAllAssignments() {
        List<Assignment> allAssignments = assignmentService.getAllAssignments();
        assigmentEntityProducerService.rePublish(allAssignments);
    }
}
