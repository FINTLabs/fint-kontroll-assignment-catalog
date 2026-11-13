package no.fintlabs.assignment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewAssignmentRequestTest {

    @ParameterizedTest
    @CsvSource(value = {
            "null, null, null, false",
            "1, null, null, true",
            "null, 2, null, true",
            "null, null, 3, true",
            "1, 2, null, false",
            "1, null, 3, false",
            "null, 2, 3, false",
            "1, 2, 3, false"
    }, nullValues = "null")
    void acceptsExactlyOneAssignee(Long userRef, Long roleRef, Long deviceGroupRef, boolean valid) {
        NewAssignmentRequest request = new NewAssignmentRequest();
        request.userRef = userRef;
        request.roleRef = roleRef;
        request.deviceGroupRef = deviceGroupRef;

        assertEquals(valid, request.isExactlyOneAssigneeRefSet());
    }
}
