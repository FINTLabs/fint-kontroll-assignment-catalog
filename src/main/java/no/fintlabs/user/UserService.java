package no.fintlabs.user;

import lombok.extern.slf4j.Slf4j;
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

    public User convertAndSaveAsUser(KontrollUser kontrollUser) {
        User convertedUser = User.builder()
                .id(kontrollUser.getId())
                .userName(kontrollUser.getUserName())
                .identityProviderUserObjectId(kontrollUser.getIdentityProviderUserObjectId())
                .firstName(kontrollUser.getFirstName())
                .lastName(kontrollUser.getLastName())
                .userType(kontrollUser.getUserType())
                .organisationUnitId(kontrollUser.getMainOrganisationUnitId())
                .organisationUnitName(kontrollUser.getMainOrganisationUnitName())
                .build();


        return save(convertedUser);
    }
}
