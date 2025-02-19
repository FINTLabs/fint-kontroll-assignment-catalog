package no.fintlabs.resource;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResourceAvailability {
    private String resourceId;
    private Long assignedResources;
    private ResourceConsumerAssignment resourceConsumerAssignment;


    @Override
    public String toString() {
        return "ResourceAvailability{" +
                "resourceId='" + resourceId + '\'' +
                ", assignedResources=" + assignedResources +
                ", resourceConsumerAssignment=" + resourceConsumerAssignment +
                '}';
    }
}