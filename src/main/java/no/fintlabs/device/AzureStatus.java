package no.fintlabs.device;

import java.util.List;

public enum AzureStatus {
    NOT_SENT,
    SENT,
    CONFIRMED,
    TO_BE_DELETED,
    DELETION_SENT,
    DELETION_CONFIRMED,
    ERROR,
    NEEDS_REPUBLISH;

    public static List<AzureStatus> activeStatuses() {
        return List.of(NOT_SENT, SENT, CONFIRMED);
    }
    public static List<AzureStatus> inactiveStatuses() {
        return List.of(TO_BE_DELETED, DELETION_SENT, DELETION_CONFIRMED);
    }
}
