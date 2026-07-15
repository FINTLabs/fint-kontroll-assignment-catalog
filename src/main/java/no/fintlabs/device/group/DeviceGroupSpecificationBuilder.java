package no.fintlabs.device.group;

import no.fintlabs.opa.OpaUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class DeviceGroupSpecificationBuilder {

    private final List<Long> deviceGroupIds;
    private final String searchString;

    public DeviceGroupSpecificationBuilder(List<Long> deviceGroupIds, String searchString) {
        this.deviceGroupIds = deviceGroupIds;
        this.searchString = searchString;
    }

    public Specification<DeviceGroup> assignmentSearch() {
        Specification<DeviceGroup> spec = belongsToDeviceGroups(deviceGroupIds);

        if (!OpaUtils.isEmptyString(searchString)) {
            spec = spec.and(nameLike(searchString.toLowerCase()));
        }

        return spec;
    }

    private Specification<DeviceGroup> belongsToDeviceGroups(List<Long> deviceGroupIds) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("id")).value(deviceGroupIds);
    }

    private Specification<DeviceGroup> nameLike(String searchString) {
        return (root, query, criteriaBuilder) -> {
            String searchPattern = "%" + searchString + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("orgUnitId")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("platform")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("deviceType")), searchPattern)
            );
        };
    }
}
