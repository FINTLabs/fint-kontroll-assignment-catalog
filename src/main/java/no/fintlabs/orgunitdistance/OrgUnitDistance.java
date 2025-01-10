package no.fintlabs.orgunitdistance;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "orgunit_distance")
public class OrgUnitDistance {
    @Id
    private String key;
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
        return distance == that.distance && Objects.equals(key, that.key) && Objects.equals(orgUnitId, that.orgUnitId) && Objects.equals(subOrgUnitId, that.subOrgUnitId);
    }


    @Override
    public int hashCode() {
        return Objects.hash(key, orgUnitId, subOrgUnitId, distance);
    }
}
