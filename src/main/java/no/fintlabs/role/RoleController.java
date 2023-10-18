package no.fintlabs.role;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class RoleController {
    private final  RoleResponseFactory roleResponseFactory;

    public RoleController( RoleResponseFactory roleResponseFactory) {
        this.roleResponseFactory = roleResponseFactory;
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
