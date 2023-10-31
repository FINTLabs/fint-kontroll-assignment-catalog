package no.fintlabs.user;


import no.fintlabs.search.SearchCriteria;
import no.fintlabs.search.SearchOperation;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserResponseFactory {
    private final AssigmentUserService assigmentUserService;
    private final UserService userService;
    public UserResponseFactory(AssigmentUserService assigmentUserService, UserService userService) {
        this.assigmentUserService = assigmentUserService;
        this.userService = userService;
    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long id,
            int pageNumber,
            int pageSize,
            String userType,
            String search
    ){
        UserSpecificationBuilder builder = new UserSpecificationBuilder();
        SearchCriteria resourceFilter  = new SearchCriteria("resourceRef","eq", id, "all");
        builder.with(resourceFilter);

        if (!userType.equals("ALLTYPES")) {
            SearchCriteria searchCriteria = new SearchCriteria("userType", "eq", (Object) userType, "all");
            builder.with(searchCriteria);
        }
        if (search != null) {
            SearchCriteria searchFirstName = new SearchCriteria("firstName", "cn", (Object) search, "all");
            builder.with(searchFirstName);
            //TODO: Find a way to use or criteria
//            SearchCriteria searchLastName = new SearchCriteria("lastName", "cn", (Object) search, "all");
//            builder.with(searchLastName);
        }
        Pageable page = PageRequest.of(pageNumber,
                pageSize,
                Sort.by("firstName").ascending()
                        .and(Sort.by("lastName")).ascending());

        Page<AssignmentUser> usersPage = assigmentUserService.findBySearchCriteria(builder.build(), page);
        return toResponseEntity(usersPage);

//        List<AssignmentUser> users = assigmentUserService.getUsersAssignedToResource(id, userType);
//        ResponseEntity<Map<String,Object>> entity = toResponseEntity(
//                toPage(users, PageRequest.of(pageNumber,pageSize)
//                )
//        );
//        return entity;
    }

    private Page<AssignmentUser> toPage(List<AssignmentUser> list, Pageable paging) {
        int start = (int) paging.getOffset();
        int end = Math.min((start + paging.getPageSize()), list.size());

        return start > list.size()
                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
                : new PageImpl<>(list.subList(start, end), paging, list.size());
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
