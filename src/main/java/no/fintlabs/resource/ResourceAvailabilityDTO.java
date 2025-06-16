package no.fintlabs.resource;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResourceAvailabilityDTO {
    private String resourceId;
    private Long assignedResources;
    private ResourceConsumerAssignmentDTO resourceConsumerAssignmentDTO;


    @Override
    public String toString() {
        return "ResourceAvailability{" +
                "resourceId='" + resourceId + '\'' +
                ", assignedResources=" + assignedResources +
                ", resourceConsumerAssignment=" + resourceConsumerAssignmentDTO +
                '}';
    }
}