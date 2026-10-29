package no.fintlabs.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import no.fintlabs.ExceptionMappingRegistry;
import no.fintlabs.ProblemDetailFactory;
import no.fintlabs.RequestBodyLogging;
import no.fintlabs.assignment.user.projection.FlattenedAssignmentService;
import no.fintlabs.assignment.user.entra.UserEntraMembershipPublisher;
import no.fintlabs.assignment.device.DeviceAssignmentService;
import no.fintlabs.assignment.device.projection.FlattenedDeviceAssignmentService;
import no.fintlabs.device.group.DeviceGroupAssignment;
import no.fintlabs.device.group.DeviceGroupRepository;
import no.fintlabs.enforcement.UpdateAssignedResourcesService;
import no.fintlabs.exception.ResourceNotFoundException;
import no.fintlabs.membership.MembershipService;
import no.fintlabs.opa.AuthManager;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.slack.SlackMessenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    private DeviceGroupRepository deviceGroupRepositoryMock;

    @MockBean
    private AssignmentResponseFactory assignmentResponseFactoryMock;

    @MockBean
    private FlattenedAssignmentService flattenedAssignmentServiceMock;

    @MockBean
    private UserEntraMembershipPublisher assigmentEntityProducerServiceMock;

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

    @MockBean
    private RequestBodyLogging requestBodyLogging;

    @SpyBean
    private ProblemDetailFactory problemDetailFactory;

    @SpyBean
    private ExceptionMappingRegistry exceptionMappingRegistry;

    @Autowired
    private WebApplicationContext context;

    private final static String ID_TOKEN = "dummyToken";

    private static PageRequest deviceGroupPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
    }


    @BeforeEach
    public void setup() throws ServletException {
        Jwt jwt = createMockJwtToken();
        createSecurityContext(jwt);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();

    }

    @Test
    public void shouldRepublishAllAssignments() throws Exception {
        when(authManagerMock.hasAdminAdminAccess(isA(Jwt.class))).thenReturn(true);

        mockMvc.perform(post("/api/assignments/republish")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).republishAllActiveUserMemberships();
    }

    @Test
    public void shouldSyncFlattenedAssignmentsWithoutPublishingPendingMemberships() throws Exception {
        Assignment assignment = new Assignment();

        when(assignmentServiceMock.getAllUserAssignments()).thenReturn(List.of(assignment));

        mockMvc.perform(post("/api/assignments/syncflattenedassignments")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).syncFlattenedAssignments(assignment, false);
    }

    @Test
    public void shouldSyncFlattenedAssignmentsAndPublishPendingMemberships() throws Exception {
        Assignment assignment = new Assignment();

        when(assignmentServiceMock.getAllUserAssignments()).thenReturn(List.of(assignment));

        mockMvc.perform(post("/api/assignments/syncflattenedassignments/publish-pending-memberships")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).syncFlattenedAssignments(assignment, true);
    }

    @Test
    public void shouldSyncFlattenedAssignmentsAndPublishPendingMembershipsByResourceId() throws Exception {
        Assignment assignment = new Assignment();
        when(assignmentServiceMock.getActiveUserAssignmentsByResource(1L)).thenReturn(List.of(assignment));

        mockMvc.perform(post("/api/assignments/syncflattenedassignments/resource/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).syncFlattenedAssignments(assignment, true);
    }

    @Test
    public void shouldSyncFlattenedAssignmentAndPublishPendingMembershipById() throws Exception {
        Assignment assignment = new Assignment();
        when(assignmentServiceMock.getAssignmentById(1L)).thenReturn(Optional.of(assignment));

        mockMvc.perform(post("/api/assignments/syncflattenedassignment/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).syncFlattenedAssignments(assignment, true);
    }

    @Test
    public void shouldSyncDirectUserAssignmentsAndPublishPendingMembershipsByUserId() throws Exception {
        Assignment assignment = new Assignment();
        when(assignmentServiceMock.getAssignmentsByUser(1L)).thenReturn(List.of(assignment));

        mockMvc.perform(post("/api/assignments/syncflattenedassignment/user/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).syncFlattenedAssignments(assignment, true);
    }

    @Test
    public void shouldRepublishUnconfirmedUserMembershipsOnCorrectlySpelledPath() throws Exception {
        mockMvc.perform(post("/api/assignments/republishunconfirmedflattenedassignments")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).republishUnconfirmedUserEntraMemberships();
    }

    @Test
    public void shouldRepublishUnconfirmedUserMembershipsByAssignmentId() throws Exception {
        mockMvc.perform(post("/api/assignments/syncunconfirmedflattenedassignment/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(flattenedAssignmentServiceMock).republishUnconfirmedUserEntraMembershipsByAssignmentId(1L);
    }

    @Test
    public void shouldUpdateMissingResourceLocationOrgUnitsForActiveUserAssignments() throws Exception {
        Assignment assignmentWithoutLocation = new Assignment();
        assignmentWithoutLocation.setId(1L);
        Assignment assignmentWithLocation = new Assignment();
        assignmentWithLocation.setId(2L);
        assignmentWithLocation.setApplicationResourceLocationOrgUnitId("existing-location");
        when(assignmentServiceMock.getAllActiveUserAssignments())
                .thenReturn(List.of(assignmentWithoutLocation, assignmentWithLocation));

        mockMvc.perform(post("/api/assignments/update-assignments-applicationresourcelocationorgunit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"updateAllResourceLocationOrgUnits\":false}"))
                .andExpect(status().isOk());

        verify(assignmentServiceMock).updateAssignmentsWithApplicationResourceLocationOrgUnit(Set.of(1L));
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
        verify(flattenedDeviceAssignmentServiceMock, never()).createAndPublishFlattenedAssignments(expectedReturnAssignment);
    }

    @Test
    public void shouldGetDeviceGroupAssignmentsByResourceId() throws Exception {
        DeviceGroupAssignment deviceGroupAssignment = DeviceGroupAssignment.builder()
                .id(20L)
                .sourceId(200L)
                .name("Device group")
                .orgUnitId("198")
                .platform("IOS")
                .deviceType("MOBILE")
                .noOfMembers(3L)
                .organisationUnitName("School A")
                .assignerUsername("assigner")
                .assignerDisplayname("Assigner Name")
                .assignmentRef(10L)
                .build();

        when(deviceAssignmentServiceMock.findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(0, 20)))
                .thenReturn(new PageImpl<>(List.of(deviceGroupAssignment), deviceGroupPageRequest(0, 20), 1));

        mockMvc.perform(get("/api/assignments/resource/1/deviceGroups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("20"))
                .andExpect(jsonPath("$.content[0].name").value("Device group"))
                .andExpect(jsonPath("$.content[0].orgUnitId").value("198"))
                .andExpect(jsonPath("$.content[0].platform").value("IOS"))
                .andExpect(jsonPath("$.content[0].deviceType").value("MOBILE"))
                .andExpect(jsonPath("$.content[0].noOfMembers").value("3"))
                .andExpect(jsonPath("$.content[0].organisationUnitName").value("School A"))
                .andExpect(jsonPath("$.content[0].assignerUsername").value("assigner"))
                .andExpect(jsonPath("$.content[0].assignerDisplayname").value("Assigner Name"))
                .andExpect(jsonPath("$.content[0].assignmentRef").value("10"))
                .andExpect(jsonPath("$.totalElements").value("1"))
                .andExpect(jsonPath("$.number").value("0"))
                .andExpect(jsonPath("$.size").value("20"));

        verify(deviceAssignmentServiceMock).findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(0, 20));
    }

    @Test
    public void shouldGetDeviceGroupAssignmentsByResourceIdWithLowercasePath() throws Exception {
        DeviceGroupAssignment deviceGroupAssignment = DeviceGroupAssignment.builder()
                .id(20L)
                .sourceId(200L)
                .name("Device group")
                .orgUnitId("198")
                .platform("IOS")
                .deviceType("MOBILE")
                .noOfMembers(3L)
                .assignerUsername("assigner")
                .assignerDisplayname("Assigner Name")
                .assignmentRef(10L)
                .build();

        when(deviceAssignmentServiceMock.findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(0, 20)))
                .thenReturn(new PageImpl<>(List.of(deviceGroupAssignment), deviceGroupPageRequest(0, 20), 1));

        mockMvc.perform(get("/api/assignments/resource/1/devicegroups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("20"))
                .andExpect(jsonPath("$.content[0].name").value("Device group"))
                .andExpect(jsonPath("$.content[0].assignerUsername").value("assigner"))
                .andExpect(jsonPath("$.content[0].assignerDisplayname").value("Assigner Name"))
                .andExpect(jsonPath("$.content[0].assignmentRef").value("10"))
                .andExpect(jsonPath("$.totalElements").value("1"))
                .andExpect(jsonPath("$.number").value("0"))
                .andExpect(jsonPath("$.size").value("20"));

        verify(deviceAssignmentServiceMock).findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(0, 20));
    }

    @Test
    public void shouldPageDeviceGroupAssignmentsByResourceId() throws Exception {
        DeviceGroupAssignment deviceGroupAssignment = DeviceGroupAssignment.builder()
                .id(21L)
                .name("Second device group")
                .build();

        when(deviceAssignmentServiceMock.findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(1, 1)))
                .thenReturn(new PageImpl<>(List.of(deviceGroupAssignment), deviceGroupPageRequest(1, 1), 2));

        mockMvc.perform(get("/api/assignments/resource/1/devicegroups?page=1&size=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("21"))
                .andExpect(jsonPath("$.content[0].name").value("Second device group"))
                .andExpect(jsonPath("$.totalElements").value("2"))
                .andExpect(jsonPath("$.totalPages").value("2"))
                .andExpect(jsonPath("$.number").value("1"))
                .andExpect(jsonPath("$.size").value("1"));

        verify(deviceAssignmentServiceMock).findDeviceGroupAssignmentsForResource(1L, null, deviceGroupPageRequest(1, 1));
    }

    @Test
    public void shouldSearchDeviceGroupAssignmentsByResourceId() throws Exception {
        DeviceGroupAssignment deviceGroupAssignment = DeviceGroupAssignment.builder()
                .id(21L)
                .name("Chromebook group")
                .assignmentRef(11L)
                .build();

        when(deviceAssignmentServiceMock.findDeviceGroupAssignmentsForResource(1L, "chrome", deviceGroupPageRequest(0, 20)))
                .thenReturn(new PageImpl<>(List.of(deviceGroupAssignment), deviceGroupPageRequest(0, 20), 1));

        mockMvc.perform(get("/api/assignments/resource/1/devicegroups?search=chrome")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("21"))
                .andExpect(jsonPath("$.content[0].name").value("Chromebook group"))
                .andExpect(jsonPath("$.content[0].assignmentRef").value("11"))
                .andExpect(jsonPath("$.totalElements").value("1"))
                .andExpect(jsonPath("$.number").value("0"))
                .andExpect(jsonPath("$.size").value("20"));

        verify(deviceAssignmentServiceMock).findDeviceGroupAssignmentsForResource(1L, "chrome", deviceGroupPageRequest(0, 20));
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
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAssignment_rejectsResourceWithoutIdentityProviderGroupObjectId() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 123L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;

        doThrow(new no.fintlabs.assignment.exception.AssignmentException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Resource 123 does not have entra group id set"))
                .when(assignmentServiceMock).createNewAssignment(123L, "99999999", null, 1L);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isUnprocessableEntity());

        verify(assignmentServiceMock).createNewAssignment(123L, "99999999", null, 1L);
    }

    @Test
    public void createAssignment_failUsedResourceNotFound() throws Exception {
        NewAssignmentRequest newAssignmentRequest = new NewAssignmentRequest();
        newAssignmentRequest.resourceRef = 123L;
        newAssignmentRequest.organizationUnitId = "99999999";
        newAssignmentRequest.roleRef = 1L;

        doThrow(new no.fintlabs.exception.ResourceNotFoundException("Resource not found: 123"))
                .when(assignmentServiceMock).createNewAssignment(123L, "99999999", null, 1L);

        mockMvc.perform(post("/api/assignments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(newAssignmentRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldDeleteAssignment_validId_deletesAssignment() throws Exception {
        Long validId = 1L;
        Assignment assignment = new Assignment();
        assignment.setId(validId);

        when(assignmentServiceMock.getAssignmentById(validId)).thenReturn(Optional.of(assignment));

        mockMvc.perform(delete("/api/assignments/" + validId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());

        verify(assignmentServiceMock, times(1)).deleteAssignment(validId);
        verify(deviceAssignmentServiceMock, never()).deleteAssignment(validId);
    }

    @Test
    public void shouldDeleteDeviceAssignmentWithDeviceAssignmentService() throws Exception {
        Long validId = 1L;
        Assignment assignment = new Assignment();
        assignment.setId(validId);
        assignment.setDeviceGroupRef(2L);

        when(assignmentServiceMock.getAssignmentById(validId)).thenReturn(Optional.of(assignment));

        mockMvc.perform(delete("/api/assignments/" + validId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone());

        verify(deviceAssignmentServiceMock, times(1)).deleteAssignment(validId);
        verify(assignmentServiceMock, never()).deleteAssignment(validId);
    }

    @Test
    public void shouldDeleteAssignment_invalidId_throwsResourceNotFoundException() throws Exception {
        Long invalidId = 2L;
        Assignment assignment = new Assignment();
        assignment.setId(invalidId);

        when(assignmentServiceMock.getAssignmentById(invalidId)).thenReturn(Optional.of(assignment));
        doThrow(new ResourceNotFoundException("User not found: 2")).when(assignmentServiceMock).deleteAssignment(invalidId);

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
