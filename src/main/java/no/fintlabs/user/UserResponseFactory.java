package no.fintlabs.user;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class UserResponseFactory {
    public static ResponseEntity<Map<String, Object>> toResponseEntity(Page<AssignmentUser> assignmentUserPage) {

        return new ResponseEntity<>(
                Map.of( "users", assignmentUserPage.getContent(),
                        "currentPage", assignmentUserPage.getNumber(),
                        "totalPages", assignmentUserPage.getTotalPages(),
                        "size", assignmentUserPage.getSize(),
                        "totalItems", assignmentUserPage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}
