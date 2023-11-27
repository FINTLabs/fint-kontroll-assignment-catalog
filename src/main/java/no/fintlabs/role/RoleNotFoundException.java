package no.fintlabs.role;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String roleRef) {
        super("Rolle med " + roleRef + " finnes ikke i assignment role-tabellen");
    }
}
