package no.fintlabs.orgunitdistance;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrgUnitDistanceService {
    private final OrgUnitDistanceRepository orgUnitDistanceRepository;

    public OrgUnitDistanceService(OrgUnitDistanceRepository orgUnitDistanceRepository) {
        this.orgUnitDistanceRepository = orgUnitDistanceRepository;
    }

    public void save(OrgUnitDistance orgUnitDistance) {
        log.info("Saving orgUnitDistance with id: {}", orgUnitDistance.getId());
        orgUnitDistanceRepository.save(orgUnitDistance);
    }

}
