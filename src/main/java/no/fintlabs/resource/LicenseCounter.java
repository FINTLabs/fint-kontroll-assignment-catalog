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


    @Override
    public String toString() {
        return "LicenseCounter{" +
                "numberOfResourcesAssignedToApplicationResourceLocation=" + numberOfResourcesAssignedToApplicationResourceLocation +
                ", applicationResourceResourceLimit=" + applicationResourceResourceLimit +
                ", numberOfResourcesAssignedToResource=" + numberOfResourcesAssignedToResource +
                ", resourceResourceLimit=" + resourceResourceLimit +
                '}';
    }
}
