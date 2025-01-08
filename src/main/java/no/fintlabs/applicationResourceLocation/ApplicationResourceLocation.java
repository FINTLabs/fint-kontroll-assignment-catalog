package no.fintlabs.applicationResourceLocation;

public record ApplicationResourceLocation(
        Long id,
        Long applicationResourceId,
        String resourceId,
        String orgUnitId,
        String orgUnitName,
        Long resourceLimit) {
}
