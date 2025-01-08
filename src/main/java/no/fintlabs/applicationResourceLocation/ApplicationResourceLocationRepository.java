package no.fintlabs.applicationResourceLocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationResourceLocationRepository extends JpaRepository<ApplicationResourceLocation,Long>, JpaSpecificationExecutor<ApplicationResourceLocation> {






}
