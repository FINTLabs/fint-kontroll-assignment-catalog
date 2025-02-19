package no.fintlabs.resource;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResourceConsumerAssignment {
    private String orgUnitId;
    private Long assignedResources;

    @Override
    public String toString() {
        return "{" +
                "orgunitId='" + orgUnitId + '\'' +
                ", assignedResources=" + assignedResources +
                '}';
    }
}
