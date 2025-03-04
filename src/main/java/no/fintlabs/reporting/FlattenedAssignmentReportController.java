package no.fintlabs.reporting;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opa.AuthorizationClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/assignments/reporting")
public class FlattenedAssignmentReportController {
    private final AuthorizationClient authorizationClient;
    private final FlattenedAssignmentReportService flattenedAssignmentReportService;

    public FlattenedAssignmentReportController(AuthorizationClient authorizationClient, FlattenedAssignmentReportService flattenedAssignmentReportService) {
        this.authorizationClient = authorizationClient;
        this.flattenedAssignmentReportService = flattenedAssignmentReportService;
    }

    @PostMapping("/generatestatisticsreport")
    public ResponseEntity<HttpStatus> generateStatisticsReport() {
        checkAdminAccess();

        flattenedAssignmentReportService.generateStatisticsReport();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void checkAdminAccess() {
        if (!authorizationClient.isAdmin()) {
            log.error("User is not authorized to perform this action");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not authorized to perform this action");
        }
    }
}
