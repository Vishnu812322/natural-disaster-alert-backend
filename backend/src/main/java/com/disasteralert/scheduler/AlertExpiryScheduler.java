package com.disasteralert.scheduler;
import com.disasteralert.disaster.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component
public class AlertExpiryScheduler {
    private final DisasterAlertRepository repo;
    public AlertExpiryScheduler(DisasterAlertRepository repo){this.repo=repo;}
    @Scheduled(fixedDelay=60000)
    public void expire(){
        repo.findByStatusOrderByCreatedAtDesc("ACTIVE").forEach(a->{
            if(a.getExpiresAt()!=null && a.getExpiresAt().isBefore(Instant.now())){
                a.setStatus("EXPIRED"); a.setUpdatedAt(Instant.now()); repo.save(a);
            }
        });
    }
}
