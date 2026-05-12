package no.fintlabs.device;

import java.util.List;

public enum EntraStatus {
    NOT_SENT,
    SENT,
    MEMBERSHIP_CONFIRMED,
    TO_BE_DELETED,
    DELETION_SENT,
    DELETION_CONFIRMED,
    ERROR,
    NEEDS_REPUBLISH;

    public static List<EntraStatus> activeStatuses() {
        return List.of(NOT_SENT, SENT, MEMBERSHIP_CONFIRMED);
    }
    public static List<EntraStatus> inactiveStatuses() {
        return List.of(TO_BE_DELETED, DELETION_SENT, DELETION_CONFIRMED);
    }
}
