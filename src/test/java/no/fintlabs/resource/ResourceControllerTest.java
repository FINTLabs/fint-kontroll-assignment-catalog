package no.fintlabs.resource;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import no.fintlabs.ExceptionMappingRegistry;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.device.assignment.DeviceAssignmentService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.opa.OpaService;
import no.fintlabs.slack.SlackMessenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResourceController.class)
public class ResourceControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private ResourceResponseFactory resourceResponseFactoryMock;

    @MockBean
    private OpaService opaServiceMock;

    @MockBean
    private AssignmentResourceService assignmentResourceServiceMock;

    @MockBean
    private ResourceService resourceServiceMock;

    @MockBean
    private DeviceAssignmentService deviceAssignmentServiceMock;

    @MockBean
    private AuthManager authManagerMock;

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

    @BeforeEach
    public void setup() throws ServletException {
        Jwt jwt = createMockJwtToken();
        createSecurityContext(jwt);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    @Test
    public void shouldGetResourceAssignmentsByDeviceGroupId() throws Exception {
        AssignmentResource resource = AssignmentResource.builder()
                .id(1L)
                .resourceId("resource-id")
                .resourceName("Resource")
                .resourceType("License")
                .assignmentRef(10L)
                .assignerUsername("assigner")
                .assignerDisplayname("Assigner Name")
                .build();

        when(deviceAssignmentServiceMock.findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(resource), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/assignments/devicegroup/20/resources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources[0].id").value("1"))
                .andExpect(jsonPath("$.resources[0].resourceId").value("resource-id"))
                .andExpect(jsonPath("$.resources[0].resourceName").value("Resource"))
                .andExpect(jsonPath("$.resources[0].resourceType").value("License"))
                .andExpect(jsonPath("$.resources[0].assignmentRef").value("10"))
                .andExpect(jsonPath("$.resources[0].assignerUsername").value("assigner"))
                .andExpect(jsonPath("$.resources[0].assignerDisplayname").value("Assigner Name"))
                .andExpect(jsonPath("$.totalItems").value("1"))
                .andExpect(jsonPath("$.currentPage").value("0"))
                .andExpect(jsonPath("$.size").value("20"));

        verify(deviceAssignmentServiceMock).findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(0, 20));
    }

    @Test
    public void shouldGetResourceAssignmentsByDeviceGroupIdWithCamelcasePath() throws Exception {
        AssignmentResource resource = AssignmentResource.builder()
                .id(1L)
                .resourceId("resource-id")
                .resourceName("Resource")
                .resourceType("License")
                .build();

        when(deviceAssignmentServiceMock.findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(resource), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/assignments/deviceGroup/20/resources")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources[0].id").value("1"))
                .andExpect(jsonPath("$.resources[0].resourceId").value("resource-id"))
                .andExpect(jsonPath("$.resources[0].resourceName").value("Resource"))
                .andExpect(jsonPath("$.resources[0].resourceType").value("License"))
                .andExpect(jsonPath("$.totalItems").value("1"))
                .andExpect(jsonPath("$.currentPage").value("0"))
                .andExpect(jsonPath("$.size").value("20"));

        verify(deviceAssignmentServiceMock).findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(0, 20));
    }

    @Test
    public void shouldPageResourceAssignmentsByDeviceGroupId() throws Exception {
        AssignmentResource resource = AssignmentResource.builder()
                .id(2L)
                .resourceId("second-resource-id")
                .resourceName("Second resource")
                .resourceType("License")
                .build();

        when(deviceAssignmentServiceMock.findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(1, 1)))
                .thenReturn(new PageImpl<>(List.of(resource), PageRequest.of(1, 1), 2));

        mockMvc.perform(get("/api/assignments/devicegroup/20/resources?page=1&size=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources[0].id").value("2"))
                .andExpect(jsonPath("$.resources[0].resourceId").value("second-resource-id"))
                .andExpect(jsonPath("$.resources[0].resourceName").value("Second resource"))
                .andExpect(jsonPath("$.totalItems").value("2"))
                .andExpect(jsonPath("$.totalPages").value("2"))
                .andExpect(jsonPath("$.currentPage").value("1"))
                .andExpect(jsonPath("$.size").value("1"));

        verify(deviceAssignmentServiceMock).findResourcesAssignedToDeviceGroup(20L, "ALLTYPES", null, null, PageRequest.of(1, 1));
    }

    private Jwt createMockJwtToken() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "none");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testUser");
        return new Jwt("dummyToken", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    private void createSecurityContext(Jwt jwt) {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
