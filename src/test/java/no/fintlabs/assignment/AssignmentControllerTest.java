package no.fintlabs.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.opa.OpaService;
import no.fintlabs.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignmentController.class)
public class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssignmentService assignmentServiceMock;

    @MockBean
    private OpaService opaServiceMock;

    @MockBean
    private AssignmentResponseFactory assignmentResponseFactoryMock;

    @MockBean
    private FlattenedAssignmentService flattenedAssignmentServiceMock;

    @MockBean
    private AssigmentEntityProducerService assigmentEntityProducerServiceMock;

    @BeforeEach
    public void setup() {
        this.mockMvc =
                MockMvcBuilders.standaloneSetup(
                        new AssignmentController(assignmentServiceMock, opaServiceMock, assignmentResponseFactoryMock,
                                                 flattenedAssignmentServiceMock, assigmentEntityProducerServiceMock)).build();
    }

    @Test
    public void republishAllAssignments() throws Exception {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();

        when(flattenedAssignmentServiceMock.getAllFlattenedAssignments()).thenReturn(List.of(flattenedAssignment));

        mockMvc.perform(post("/api/assignments/republish")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(assigmentEntityProducerServiceMock).publish(flattenedAssignment);
    }

    @Test
    public void createAssignment() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 1L;
        newAssignmentRequest.organizationUnitId = "99999999";

        Assignment expectedReturnAssignment = new Assignment();
        expectedReturnAssignment.setId(1L);
        when(assignmentServiceMock.createNewAssignment(isA(Assignment.class))).thenReturn(expectedReturnAssignment);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.id").value("1"));

        verify(assignmentServiceMock).createNewAssignment(isA(Assignment.class));
    }

    @Test
    public void deleteAssignment_ValidId_DeletesAssignment() throws Exception {
        Long validId = 1L;

        mockMvc.perform(delete("/api/assignments/" + validId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());

        verify(assignmentServiceMock, times(1)).deleteAssignment(validId);
    }

    @Test
    public void deleteAssignment_InvalidId_ThrowsUserNotFoundException() throws Exception {
        Long invalidId = 2L;

        doThrow(UserNotFoundException.class).when(assignmentServiceMock).deleteAssignment(invalidId);

        mockMvc.perform(delete("/api/assignments/" + invalidId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(assignmentServiceMock, times(1)).deleteAssignment(invalidId);
    }
}
