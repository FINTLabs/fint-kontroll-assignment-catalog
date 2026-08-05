package no.fintlabs.reporting;

import io.swagger.v3.oas.annotations.Operation;
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
    private static final String DEVELOPER_ENDPOINTS_TAG = "Developer endpoints";

    private final FlattenedAssignmentReportService flattenedAssignmentReportService;

    public FlattenedAssignmentReportController(FlattenedAssignmentReportService flattenedAssignmentReportService) {
        this.flattenedAssignmentReportService = flattenedAssignmentReportService;
    }

    @OnlyDevelopers
    @Operation(
            tags = DEVELOPER_ENDPOINTS_TAG,
            summary = "Generate flattened assignment statistics report",
            description = "Generates the flattened assignment statistics report."
    )
    @PostMapping("/generatestatisticsreport")
    public ResponseEntity<HttpStatus> generateStatisticsReport() {
        flattenedAssignmentReportService.generateStatisticsReport();

        return new ResponseEntity<>(HttpStatus.OK);
    }


}
