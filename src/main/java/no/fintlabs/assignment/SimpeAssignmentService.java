package no.fintlabs.assignment;

import org.springframework.stereotype.Service;

@Service
public class SimpeAssignmentService {
    private final AssigmentEntityProducerService assigmentEntityProducerService;

    public SimpeAssignmentService(AssigmentEntityProducerService assigmentEntityProducerService) {
        this.assigmentEntityProducerService = assigmentEntityProducerService;
    }

    public void process(Assignment assignment) {
        assigmentEntityProducerService.publish(assignment);
    }

    public void processDeletion(Assignment assignment) {
        assigmentEntityProducerService.publishDeletion(assignment);
    }
}
