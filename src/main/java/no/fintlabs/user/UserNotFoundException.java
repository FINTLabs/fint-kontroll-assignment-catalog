package no.fintlabs.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userRef) {
        super("Bruker med " + userRef + " finnes ikke i assignment users-tabellen");
    }
}
