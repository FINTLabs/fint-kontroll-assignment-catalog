package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.ResourceService;
import no.fintlabs.resource.ResourceResponseFactory;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
public class UserController {
    private final UserResponseFactory userResponseFactory;

    public UserController(UserResponseFactory userResponseFactory) {
        this.userResponseFactory = userResponseFactory;
    }
    @GetMapping("resource/{id}/users")
    public ResponseEntity<Map<String , Object>> getUsersByResourceId(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable Long id,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "${fint.kontroll.assignment-catalog.pagesize:20}") int size,
                                                                     @RequestParam(value = "userType", defaultValue = "ALLTYPES") String userType,
                                                                     @RequestParam(value= "orgUnits", required = false) List<String> orgUnits,
                                                                     @RequestParam(value = "search", required = false) String search
                                                                     ){
        log.info("Fetching users for resource with Id: " +id);
        return userResponseFactory.toResponseEntity(id, userType, orgUnits, search, page,size);
    }
}
