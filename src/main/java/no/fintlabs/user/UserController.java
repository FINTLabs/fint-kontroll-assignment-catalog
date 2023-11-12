package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.opautils.OpaUtils;
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
    private final OpaUtils opaUtils;

    public UserController(UserResponseFactory userResponseFactory, OpaUtils opaUtils) {
        this.userResponseFactory = userResponseFactory;
        this.opaUtils = opaUtils;
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
        List<String> orgUnitsInScope = opaUtils.getOrgUnitsInScope();
        log.info("Org units returned from scope: {}", orgUnitsInScope);
        log.info("Fetching users for resource with Id: " +id);
        return userResponseFactory.toResponseEntity(id, userType, orgUnits, search, page,size);
    }
}
