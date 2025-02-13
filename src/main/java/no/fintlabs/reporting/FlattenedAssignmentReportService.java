package no.fintlabs.reporting;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.assignment.flattened.FlattenedAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Slf4j
@Service
public class FlattenedAssignmentReportService {
    private final FlattenedAssignmentRepository flattenedAssignmentRepository;
    private final FlattenedAssignmentReportProducer flattenedAssignmentReportProducer;

    public FlattenedAssignmentReportService(FlattenedAssignmentRepository flattenedAssignmentRepository, FlattenedAssignmentReportProducer flattenedAssignmentReportProducer) {
        this.flattenedAssignmentRepository = flattenedAssignmentRepository;
        this.flattenedAssignmentReportProducer = flattenedAssignmentReportProducer;
    }

//    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Oslo") // Every day at midnight //TODO: activate when testet in production as ok
    @Transactional
    public void generateStatisticsReport() {
        log.info("Publishing data for statistics report");

        try (Stream<FlattenedAssignmentReport> allFlattenedStream = flattenedAssignmentRepository.streamAllFlattenedAssignmentsForReport()) {
            allFlattenedStream.forEach(flattenedAssignmentReportProducer::publish);
        }

        log.info("Done publishing data for statistics report");
    }
}
