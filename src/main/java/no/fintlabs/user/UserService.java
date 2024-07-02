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

    public User convertAndSaveAsUser(User userUpdate) {
        Optional<User> existingUserOptional = userRepository.findById(userUpdate.getId());

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            if (!existingUser.equals(userUpdate)) {
                return save(userUpdate);
            } else {
                log.info("User {} already exists and is equal to the incoming user. Skipping.", userUpdate.getId());
                return existingUser;
            }
        } else {
            return save(userUpdate);
        }
    }
}
