package no.fintlabs.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.opa.OpaService;
import no.fintlabs.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @MockBean
    private MembershipService membershipServiceMock;

    @MockBean
    private AuthManager authManagerMock;

    @Autowired
    private WebApplicationContext context;

    private final static String ID_TOKEN = "dummyToken";


    @BeforeEach
    public void setup() throws ServletException {
        Jwt jwt = createMockJwtToken();
        createSecurityContext(jwt);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();

    }

    @Test
    public void shouldRepublishAllAssignments() throws Exception {
        FlattenedAssignment flattenedAssignment = new FlattenedAssignment();

        when(authManagerMock.hasAdminAdminAccess(isA(Jwt.class))).thenReturn(true);
        when(flattenedAssignmentServiceMock.getAllFlattenedAssignments()).thenReturn(List.of(flattenedAssignment));

        mockMvc.perform(post("/api/assignments/republish")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(assigmentEntityProducerServiceMock).rePublish(flattenedAssignment);
    }

    @Test
    public void shouldCreateAssignment() throws Exception {
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
    public void shouldDeleteAssignment_validId_deletesAssignment() throws Exception {
        Long validId = 1L;

        mockMvc.perform(delete("/api/assignments/" + validId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());

        verify(assignmentServiceMock, times(1)).deleteAssignment(validId);
    }

    @Test
    public void shouldDeleteAssignment_invalidId_throwsUserNotFoundException() throws Exception {
        Long invalidId = 2L;

        doThrow(UserNotFoundException.class).when(assignmentServiceMock).deleteAssignment(invalidId);

        mockMvc.perform(delete("/api/assignments/" + invalidId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(assignmentServiceMock, times(1)).deleteAssignment(invalidId);
    }

    private void createSecurityContext(Jwt jwt) throws ServletException {
        SecurityContextHolder.getContext().setAuthentication(createJwtAuthentication(jwt));
        SecurityContextHolderAwareRequestFilter authInjector = new SecurityContextHolderAwareRequestFilter();
        authInjector.afterPropertiesSet();
    }

    private UsernamePasswordAuthenticationToken createJwtAuthentication(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt, null, authorities);
        return authentication;
    }

    private Jwt createMockJwtToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of("authenticated", "ROLE_ADMIN"));
        Jwt jwt = new Jwt(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims, claims);
        return jwt;
    }
}
