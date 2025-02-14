package no.fintlabs.resource;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class ResourceConsumerAssignments {
    private Long id;
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
