package no.fintlabs.assignment.flattened;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FlattenedAssignmentMapperTest {

    private final FlattenedAssignmentMapper mapper = new FlattenedAssignmentMapper();

    @Test
    void mapOriginWithExisting_shouldCopyTerminationReasonWhenTerminationDateIsCopied() {
        Date terminationDate = new Date();
        FlattenedAssignment origin = new FlattenedAssignment();
        origin.setAssignmentId(1L);
        origin.setUserRef(10L);
        origin.setResourceRef(20L);
        origin.setAssignmentTerminationDate(terminationDate);
        origin.setAssignmentTerminationReason("User is no longer active");

        FlattenedAssignment existing = new FlattenedAssignment();
        existing.setAssignmentId(1L);
        existing.setUserRef(10L);
        existing.setResourceRef(20L);

        Optional<FlattenedAssignment> mapped = mapper.mapOriginWithExisting(origin, List.of(existing));

        assertThat(mapped).containsSame(existing);
        assertThat(existing.getAssignmentTerminationDate()).isEqualTo(terminationDate);
        assertThat(existing.getAssignmentTerminationReason()).isEqualTo("User is no longer active");
    }
}
