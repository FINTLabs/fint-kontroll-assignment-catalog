package no.fintlabs.applicationresourcelocation;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@EqualsAndHashCode
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
    @EqualsAndHashCode.Exclude
    Long numberOfResourcesAssigned;



}
