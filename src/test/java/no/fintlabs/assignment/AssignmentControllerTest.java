package no.fintlabs.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
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
import java.util.Optional;
import java.util.UUID;

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
    private ResourceRepository resourceRepositoryMock;

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
    public void shouldCreateValidUserAssignment() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 1L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.userRef = 1L;

        Assignment expectedReturnAssignment = new Assignment();
        expectedReturnAssignment.setId(1L);
        expectedReturnAssignment.setResourceRef(1L);
        expectedReturnAssignment.setOrganizationUnitId("99999999");
        expectedReturnAssignment.setUserRef(1L);

        Resource value = new Resource();
        value.setId(1L);
        value.setIdentityProviderGroupObjectId(UUID.randomUUID());

        when(resourceRepositoryMock.findById(1L)).thenReturn(Optional.of(value));
        when(assignmentServiceMock.createNewAssignment(1L, "99999999", 1L, null)).thenReturn(expectedReturnAssignment);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.id").value("1"));

        verify(assignmentServiceMock).createNewAssignment(1L, "99999999", 1L, null);
    }

    @Test
    public void shouldCreateValidGroupAssignment() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 1L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;

        Assignment expectedReturnAssignment = new Assignment();
        expectedReturnAssignment.setId(1L);
        expectedReturnAssignment.setResourceRef(1L);
        expectedReturnAssignment.setOrganizationUnitId("99999999");
        expectedReturnAssignment.setRoleRef(1L);

        Resource value = new Resource();
        value.setId(1L);
        value.setIdentityProviderGroupObjectId(UUID.randomUUID());

        when(resourceRepositoryMock.findById(1L)).thenReturn(Optional.of(value));
        when(assignmentServiceMock.createNewAssignment(1L, "99999999", null, 1L)).thenReturn(expectedReturnAssignment);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.organizationUnitId").value("99999999"))
                .andExpect(jsonPath("$.roleRef").value("1"));

        verify(assignmentServiceMock).createNewAssignment(1L, "99999999", null, 1L);
    }

    @Test
    public void createAssignment_failOnBothRoleAndUser() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 1L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;
        newAssignmentRequest.userRef = 1L;

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("Cannot assign both role and user"));
    }

    @Test
    public void createAssignment_failMissingResource() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = null;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;
        newAssignmentRequest.userRef = 1L;

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("ResourceRef must be set"));
    }

    @Test
    public void createAssignment_failMissingIdentityProviderGroupObjectId() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 123L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;
        newAssignmentRequest.userRef = 1L;

        Resource value = new Resource();
        value.setId(1L);
        value.setIdentityProviderGroupObjectId(null);

        when(resourceRepositoryMock.findById(1L)).thenReturn(Optional.of(value));

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("Resource 1 does not have azure group id set"));
    }

    @Test
    public void createAssignment_failUsedResourceNotFound() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 123L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;
        newAssignmentRequest.userRef = 1L;

        Resource value = new Resource();
        value.setId(1L);
        value.setIdentityProviderGroupObjectId(null);

        when(resourceRepositoryMock.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> result.getResponse().getContentAsString().contains("Resource 1 not found"));
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
