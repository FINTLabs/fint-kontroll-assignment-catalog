package no.fintlabs.resource;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceRef) {
        super("Ressurs med " + resourceRef + " finnes ikke i assignment resource-tabellen");
    }
}
