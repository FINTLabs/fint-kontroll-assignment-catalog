package no.fintlabs.resource;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LicenseCounter {
    Long numberOfResourcesAssignedToApplicationResourceLocation;
    Long applicationResourceResourceLimit;
    Long numberOfResourcesAssignedToResource;
    Long resourceResourceLimit;
}
