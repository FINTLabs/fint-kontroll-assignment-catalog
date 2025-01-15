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
}
