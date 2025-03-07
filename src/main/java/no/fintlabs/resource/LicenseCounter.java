package no.fintlabs.resource;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LicenseCounter {
    Long numberOfResourcesAssignedToApplicationResourceLocation;
    Long applicationResourceResourceLimit;
    Long numberOfResourcesAssignedToResource;
    Long resourceResourceLimit;
}
