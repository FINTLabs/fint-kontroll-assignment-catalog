package no.fintlabs.resource;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ResourceAvailability {
    private String resourceId;
    private Long assignedResources;
    private List<ResourceConsumerAssignments> resourceConsumerAssignments;


    @Override
    public String toString() {
        return "ResourceAvailability{" +
                "resourceId='" + resourceId + '\'' +
                ", assignedResources=" + assignedResources +
                ", resourceConsumerAssignments=" + resourceConsumerAssignments +
                '}';
    }
}