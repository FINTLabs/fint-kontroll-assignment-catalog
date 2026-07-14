package no.fintlabs.device.group;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class DeviceGroupAssignment {
    private Long id;
    private Long sourceId;
    private String name;
    private String orgUnitId;
    private String platform;
    private String deviceType;
    private Date createdDate;
    private Date modifiedDate;
    private long noOfMembers;

    private Long assignmentRef;
    private String organizationUnitId;
    private String organisationUnitName;
    private String assignerUsername;
    private String assignerDisplayname;
    private Date assignmentDate;
}
