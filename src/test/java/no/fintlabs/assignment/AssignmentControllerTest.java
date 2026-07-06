package no.fintlabs.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import no.fintlabs.ExceptionMappingRegistry;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.assignment.flattened.FlattenedAssignment;
import no.fintlabs.assignment.flattened.FlattenedAssignmentService;
import no.fintlabs.device.assignment.DeviceAssignmentService;
import no.fintlabs.device.assignment.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroup;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.resource.Resource;
import no.fintlabs.resource.ResourceRepository;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.slack.SlackMessenger;
import no.fintlabs.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssignmentController.class)
public class AssignmentControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private AssignmentService assignmentServiceMock;

    @MockBean
    private ResourceService resourceServiceMock;

    @MockBean
    private DeviceAssignmentService deviceAssignmentServiceMock;

    @MockBean
    private FlattenedDeviceAssignmentService flattenedDeviceAssignmentServiceMock;

    @MockBean
    private ResourceRepository resourceRepositoryMock;

    @MockBean
    private DeviceGroupRepository deviceGroupRepositoryMock;

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

    @MockBean
    private UpdateAssignedResourcesService updateAssignedResourcesServiceMock;

    @MockBean
    private SlackMessenger slackMessengerMock;

    @MockBean
    private Tracer tracer;

    @SpyBean
    private ProblemDetailFactory problemDetailFactory;

    @SpyBean
    private ExceptionMappingRegistry exceptionMappingRegistry;

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
    public void shouldCreateValidDeviceGroupAssignment() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 6L;
        newAssignmentRequest.deviceGroupRef = 2L;
        newAssignmentRequest.organizationUnitId = "198";

        Assignment expectedReturnAssignment = new Assignment();
        expectedReturnAssignment.setId(1L);
        expectedReturnAssignment.setResourceRef(6L);
        expectedReturnAssignment.setOrganizationUnitId("198");
        expectedReturnAssignment.setDeviceGroupRef(2L);

        Resource value = new Resource();
        value.setId(6L);
        value.setIdentityProviderGroupObjectId(UUID.randomUUID());

        when(resourceRepositoryMock.findById(6L)).thenReturn(Optional.of(value));
        when(deviceAssignmentServiceMock.createNewAssignment(6L, "198", 2L)).thenReturn(expectedReturnAssignment);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.resourceRef").value("6"))
                .andExpect(jsonPath("$.deviceGroupRef").value("2"))
                .andExpect(jsonPath("$.organizationUnitId").value("198"));

        verify(deviceAssignmentServiceMock).createNewAssignment(6L, "198", 2L);
        verify(flattenedDeviceAssignmentServiceMock).createAndPublishFlattenedAssignments(expectedReturnAssignment);
    }

    @Test
    public void shouldGetDeviceGroupAssignmentsByResourceId() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setResourceRef(1L);
        assignment.setResourceName("Resource");
        assignment.setDeviceGroupRef(20L);

        DeviceGroup deviceGroup = DeviceGroup.builder()
                .id(20L)
                .sourceId(200L)
                .name("Device group")
                .orgUnitId("198")
                .platform("IOS")
                .deviceType("MOBILE")
                .noOfMembers(3L)
                .build();

        when(deviceAssignmentServiceMock.getActiveAssignmentsByResource(1L)).thenReturn(List.of(assignment));
        when(deviceGroupRepositoryMock.findAllById(List.of(20L))).thenReturn(List.of(deviceGroup));

        mockMvc.perform(get("/api/assignments/resource/1/deviceGroups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("20"))
                .andExpect(jsonPath("$[0].name").value("Device group"))
                .andExpect(jsonPath("$[0].orgUnitId").value("198"))
                .andExpect(jsonPath("$[0].platform").value("IOS"))
                .andExpect(jsonPath("$[0].deviceType").value("MOBILE"))
                .andExpect(jsonPath("$[0].noOfMembers").value("3"));

        verify(deviceAssignmentServiceMock).getActiveAssignmentsByResource(1L);
        verify(deviceGroupRepositoryMock).findAllById(List.of(20L));
    }

    @Test
    public void shouldGetResourceAssignmentsByDeviceGroupId() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setId(10L);
        assignment.setResourceRef(1L);
        assignment.setResourceName("Resource");
        assignment.setDeviceGroupRef(20L);

        Resource resource = new Resource();
        resource.setId(1L);
        resource.setResourceId("resource-id");
        resource.setResourceName("Resource");
        resource.setResourceType("License");

        when(deviceAssignmentServiceMock.getActiveAssignmentsByDeviceGroup(20L)).thenReturn(List.of(assignment));
        when(resourceRepositoryMock.findAllById(List.of(1L))).thenReturn(List.of(resource));

        mockMvc.perform(get("/api/assignments/devicegroup/20/resources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].resourceId").value("resource-id"))
                .andExpect(jsonPath("$[0].resourceName").value("Resource"))
                .andExpect(jsonPath("$[0].resourceType").value("License"));

        verify(deviceAssignmentServiceMock).getActiveAssignmentsByDeviceGroup(20L);
        verify(resourceRepositoryMock).findAllById(List.of(1L));
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
    public void createAssignment_failMissingIdentityProviderGroupObjectId() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 123L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;

        Resource value = new Resource();
        value.setId(1L);
        value.setIdentityProviderGroupObjectId(null);

        when(resourceRepositoryMock.findById(123L)).thenReturn(Optional.of(value));

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("Resource 123 does not have azure group id set")));
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
