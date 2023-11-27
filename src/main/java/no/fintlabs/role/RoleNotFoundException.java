package no.fintlabs.role;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String roleRef) {
        super("Bruker med " + roleRef + " finnes ikke i assignment role-tabellen");
    }
}
