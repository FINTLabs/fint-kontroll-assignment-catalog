package no.fintlabs.user;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserResponseFactory {
    private final UserRepository userRepository;
    public UserResponseFactory(UserRepository userRepository) {

        this.userRepository = userRepository;

    }
    public ResponseEntity<Map<String ,Object>> toResponseEntity(
            Long id,
            int page,
            int size){
        List<SimpleUser> users = userRepository
                .getUsersByResourceId(id)
                .stream()
                .map(User::toSimpleUser)
                .toList();
        ResponseEntity<Map<String,Object>> entity = toResponseEntity(
                toPage(users, PageRequest.of(page,size)
                )
        );
        return entity;
    }

    private Page<SimpleUser> toPage(List<SimpleUser> list, Pageable paging) {
        int start = (int) paging.getOffset();
        int end = Math.min((start + paging.getPageSize()), list.size());

        return start > list.size()
                ? new PageImpl<>(new ArrayList<>(), paging, list.size())
                : new PageImpl<>(list.subList(start, end), paging, list.size());
    }
    public ResponseEntity<Map<String, Object>> toResponseEntity(Page<SimpleUser> userPage) {

        return new ResponseEntity<>(
                Map.of( "users", userPage.getContent(),
                        "currentPage", userPage.getNumber(),
                        "totalPages", userPage.getTotalPages(),
                        "size", userPage.getSize(),
                        "totalItems", userPage.getTotalElements()
                ),
                HttpStatus.OK
        );
    }
}
