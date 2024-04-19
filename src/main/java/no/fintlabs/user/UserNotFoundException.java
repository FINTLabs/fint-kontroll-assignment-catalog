package no.fintlabs.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userRef) {
        super(String.format("User with ref %s does not exist in users", userRef));
    }

    public UserNotFoundException(String userName) {
        super(String.format("User with username %s does not exist", userName));
    }
}
