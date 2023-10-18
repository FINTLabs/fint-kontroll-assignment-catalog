package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.role.RoleResponseFactory;
import no.fintlabs.user.UserResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class ResourceController {
    private final UserResponseFactory userResponseFactory;
    private final RoleResponseFactory roleResponseFactory;
    private final ResourceService resourceService;

    public ResourceController(UserResponseFactory userResponseFactory, RoleResponseFactory roleResponseFactory, ResourceService resourceService) {
        this.userResponseFactory = userResponseFactory;
        this.roleResponseFactory = roleResponseFactory;
        this.resourceService = resourceService;
    }

    @GetMapping("resource/{id}/users")
    public ResponseEntity<Map<String , Object>> getUsersByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                                   @PathVariable Long id,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size){
        log.info("Fetching users for resource with Id: " +id);
        return userResponseFactory.toResponseEntity(id,page,size);
    }
    @GetMapping("resource/{id}/roles")
    public ResponseEntity<Map<String , Object>> getRolesByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size){
        log.info("Fetching roles for resource with Id: " +id);
        return roleResponseFactory.toResponseEntity(id,page,size);
    }

}
