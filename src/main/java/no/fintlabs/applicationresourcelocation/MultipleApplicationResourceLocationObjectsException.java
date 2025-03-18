package no.fintlabs.applicationresourcelocation;

public class MultipleApplicationResourceLocationObjectsException extends RuntimeException {
    public MultipleApplicationResourceLocationObjectsException(String orgUnit, String resourceId) {
        super("Multiple applicationResourceLocation objects found for resourceId: " + resourceId + " and " + orgUnit);
    }
}
