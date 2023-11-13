package no.fintlabs.user;

import no.fintlabs.utils.Utils;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserResponseFactory {
    private final AssigmentUserService assigmentUserService;
    private final UserService userService;
    private final Utils utils;
    public UserResponseFactory(AssigmentUserService assigmentUserService, UserService userService, Utils utils) {
        this.assigmentUserService = assigmentUserService;
        this.userService = userService;
        this.utils = utils;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long resourceId,
            String userType,
            List<String> orgUnits,
            List<String> orgUnitsInScope,
            String searchString,
            int pageNumber,
            int pageSize
    ){
        UserSpecificationBuilder builder = new UserSpecificationBuilder(resourceId, userType, orgUnits, orgUnitsInScope,searchString, utils);

        Pageable page = PageRequest.of(pageNumber,
                pageSize,
                Sort.by("firstName").ascending()
                        .and(Sort.by("lastName")).ascending());

        Page<AssignmentUser> usersPage = assigmentUserService.findBySearchCriteria(resourceId, builder.build(), page);
        return toResponseEntity(usersPage);
    }
    public ResponseEntity<Map<String, Object>> toResponseEntity(Page<AssignmentUser> assignmentUserPage) {

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
