package no.fintlabs.resource;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.user.UserResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignmentresources")
public class ResourceController {
    private final UserResponseFactory userResponseFactory;
    private final ResourceService resourceService;

    public ResourceController(UserResponseFactory userResponseFactory, ResourceService resourceService) {
        this.userResponseFactory = userResponseFactory;
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

}
