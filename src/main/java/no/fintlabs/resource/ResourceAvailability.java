package no.fintlabs.resource;

import jakarta.persistence.*;

import java.util.List;


public class ResourceAvailability {
    private Long id;
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