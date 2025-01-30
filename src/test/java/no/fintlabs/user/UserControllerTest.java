package no.fintlabs.user;

import jakarta.servlet.ServletException;
import no.fintlabs.opa.OpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @MockBean
    private AssignmentUserService assigmentUserServiceMock;

    @MockBean
    private OpaService opaServiceMock;
//
//    @MockBean
//    private AssignmentResponseFactory assignmentResponseFactoryMock;
//
//    @MockBean
//    private FlattenedAssignmentService flattenedAssignmentServiceMock;
//
//    @MockBean
//    private AssigmentEntityProducerService assigmentEntityProducerServiceMock;
//
//    @MockBean
//    private AuthManager authManagerMock;

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
    public void shouldGetUsersByResourceId2() throws Exception {
        ResourceAssignmentUser resourceAssignmentUser = ResourceAssignmentUser.builder()
                .assigneeRef(1L)
                .build();

        String scope = "user";
        when(opaServiceMock.getOrgUnitsInScope(scope)).thenReturn(List.of("99999999"));
        when(assigmentUserServiceMock.findResourceAssignmentUsersForResourceId(isA(Long.class), isA(String.class), isNull(), anyList(), isNull(), isNull(), isA(Integer.class), isA(Integer.class)))
                .thenReturn(new PageImpl<>(List.of(resourceAssignmentUser)));

        mockMvc.perform(get("/api/assignments/v2/resource/1/users")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.users").isNotEmpty())
                .andExpect(jsonPath("$.users[0].assigneeRef").value(1L));

        verify(opaServiceMock, times(1)).getOrgUnitsInScope(scope);
        verify(assigmentUserServiceMock, times(1)).findResourceAssignmentUsersForResourceId(isA(Long.class), isA(String.class), isNull(), anyList(), isNull(), isNull(), isA(Integer.class), isA(Integer.class));
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
        claims.put("roles", List.of("authenticated", "ROLE_USER"));
        Jwt jwt = new Jwt(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims, claims);
        return jwt;
    }
}
