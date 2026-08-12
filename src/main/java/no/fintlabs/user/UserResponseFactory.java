package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.device.group.DeviceGroupAssignment;
import no.fintlabs.role.AssignmentRole;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Slf4j
public class UserResponseFactory {
    public static ResponseEntity<Map<String, Object>> assignmentUsersToResponseEntity(Page<AssignmentUser> assignmentUserPage) {

        return new ResponseEntity<>(
                Map.of("users", assignmentUserPage.getContent(),
                        "currentPage", assignmentUserPage.getNumber(),
                        "totalPages", assignmentUserPage.getTotalPages(),
                        "size", assignmentUserPage.getSize(),
                        "totalItems", assignmentUserPage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }

    public static ResponseEntity<Map<String, Object>> resourceAssignmentUsersToResponseEntity(Page<ResourceAssignmentUser> resourceAssignmentUsersPagable) {

        return new ResponseEntity<>(
                Map.of("users", resourceAssignmentUsersPagable.getContent(),
                        "currentPage", resourceAssignmentUsersPagable.getNumber(),
                        "totalPages", resourceAssignmentUsersPagable.getTotalPages(),
                        "size", resourceAssignmentUsersPagable.getSize(),
                        "totalItems", resourceAssignmentUsersPagable.getTotalElements()
                ),
                HttpStatus.OK
        );
    }

    public static ResponseEntity<Map<String, Object>> assignmentRoleToResponseEntity(Page<AssignmentRole> rolePage) {

        return new ResponseEntity<>(
                Map.of("roles", rolePage.getContent(),
                        "currentPage", rolePage.getNumber(),
                        "totalPages", rolePage.getTotalPages(),
                        "size", rolePage.getSize(),
                        "totalItems", rolePage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }

    public static ResponseEntity<Map<String, Object>> deviceGroupAssignmentToResponseEntity(Page<DeviceGroupAssignment> deviceGroupAssignments){
        return new ResponseEntity<>(
                Map.of("deviceGroupAssignments", deviceGroupAssignments.getContent(),
                        "currentPage", deviceGroupAssignments.getNumber(),
                        "totalPages", deviceGroupAssignments.getTotalPages(),
                        "size", deviceGroupAssignments.getSize(),
                        "totalItems", deviceGroupAssignments.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}
