package no.fintlabs.applicationresourcelocation;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "application_resource_location")
public class ApplicationResourceLocation {

    @Id
    Long id;
    @Column(name = "applicationresourceid")
    Long applicationResourceId;
    @Column(name = "resourceid")
    String resourceId;
    @Column(name = "orgunitid")
    String orgUnitId;
    @Column(name = "orgunitname")
    String orgUnitName;
    @Column(name = "resourcelimit")
    Long resourceLimit;
    @Column(name="numberofresourcesassigned")
    Long numberOfResourcesAssigned;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationResourceLocation that = (ApplicationResourceLocation) o;
        return Objects.equals(id, that.id) && Objects.equals(applicationResourceId, that.applicationResourceId)
                && Objects.equals(resourceId, that.resourceId) && Objects.equals(orgUnitId, that.orgUnitId)
                && Objects.equals(orgUnitName, that.orgUnitName) && Objects.equals(resourceLimit, that.resourceLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, applicationResourceId, resourceId, orgUnitId, orgUnitName, resourceLimit);
    }
}
