package no.fintlabs.orgunitdistance;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "orgunit_distance")
public class OrgUnitDistance {
    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "orgunitid")
    private String orgUnitId;
    @Column(name = "suborgunitid")
    private String subOrgUnitId;
    @Column(name = "distance")
    private int distance;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgUnitDistance that = (OrgUnitDistance) o;
        return distance == that.distance && Objects.equals(id, that.id) && Objects.equals(orgUnitId, that.orgUnitId) && Objects.equals(subOrgUnitId, that.subOrgUnitId);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, orgUnitId, subOrgUnitId, distance);
    }
}
