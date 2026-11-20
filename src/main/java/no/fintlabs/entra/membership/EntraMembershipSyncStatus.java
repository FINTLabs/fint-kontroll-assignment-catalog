package no.fintlabs.entra.membership;

import java.util.List;

public enum EntraMembershipSyncStatus {
    NOT_SENT,
    SENT,
    MEMBERSHIP_CONFIRMED,
    TO_BE_DELETED,
    DELETION_SENT,
    DELETION_CONFIRMED,
    ERROR,
    NEEDS_REPUBLISH;

    public static List<EntraMembershipSyncStatus> activeStatuses() {
        return List.of(NOT_SENT, SENT, MEMBERSHIP_CONFIRMED);
    }

    public static List<EntraMembershipSyncStatus> inactiveStatuses() {
        return List.of(TO_BE_DELETED, DELETION_SENT, DELETION_CONFIRMED);
    }
}
