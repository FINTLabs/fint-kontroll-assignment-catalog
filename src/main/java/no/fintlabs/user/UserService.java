package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }
    public Page<User> findBySearchCriteria(Specification<User> spec, Pageable page){
        Page<User> searchResult = userRepository.findAll(spec, page);
        return searchResult;
    }
}
