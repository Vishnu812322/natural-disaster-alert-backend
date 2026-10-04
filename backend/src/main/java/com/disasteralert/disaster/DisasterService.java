package com.disasteralert.disaster;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
@Service
public class DisasterService {
    private final DisasterAlertRepository repo;
    public DisasterService(DisasterAlertRepository repo){this.repo=repo;}
    public List<DisasterAlert> all(){return repo.findAllByOrderByCreatedAtDesc();}
    public List<DisasterAlert> active(){return repo.findByStatusOrderByCreatedAtDesc("ACTIVE");}
    public DisasterAlert create(CreateAlertRequest r){
        DisasterAlert a=new DisasterAlert();
        a.setTitle(r.title()); a.setDescription(r.description()); a.setDisasterType(r.disasterType());
        a.setSeverity(r.severity()); a.setSource(r.source()); a.setSourceAlertId(r.sourceAlertId());
        a.setCenterLatitude(r.centerLatitude()); a.setCenterLongitude(r.centerLongitude()); a.setRadiusKm(r.radiusKm());
        a.setAreaText(r.areaText()); a.setInstructions(r.instructions()); a.setTestAlert(r.testAlert());
        a.setEffectiveAt(r.effectiveAt()==null?Instant.now():r.effectiveAt());
        a.setExpiresAt(r.expiresAt()); a.setStatus("ACTIVE");
        return repo.save(a);
    }
    public DisasterAlert close(Long id){ DisasterAlert a=repo.findById(id).orElseThrow(); a.setStatus("RESOLVED"); a.setUpdatedAt(Instant.now()); return repo.save(a); }
}
