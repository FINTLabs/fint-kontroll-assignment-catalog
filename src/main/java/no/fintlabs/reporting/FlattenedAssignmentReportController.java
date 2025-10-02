package no.fintlabs.reporting;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.util.OnlyDevelopers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/assignments/reporting")
public class FlattenedAssignmentReportController {
    private final FlattenedAssignmentReportService flattenedAssignmentReportService;

    public FlattenedAssignmentReportController(FlattenedAssignmentReportService flattenedAssignmentReportService) {
        this.flattenedAssignmentReportService = flattenedAssignmentReportService;
    }

    @OnlyDevelopers
    @PostMapping("/generatestatisticsreport")
    public ResponseEntity<HttpStatus> generateStatisticsReport() {
        flattenedAssignmentReportService.generateStatisticsReport();

        return new ResponseEntity<>(HttpStatus.OK);
    }


}
