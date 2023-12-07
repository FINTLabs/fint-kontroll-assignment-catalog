package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    public Optional<String> getUserDisplaynameByUsername(String username) {
        String displayname = null;
        Optional<User> user = userRepository.findByUserName(username);

        if (user.isPresent() && user.get().getFirstName()!=null && user.get().getLastName()!=null) {
            displayname = user.get().getFirstName() + " " + user.get().getLastName();
        }

        return Optional.of(displayname);
    }
}
