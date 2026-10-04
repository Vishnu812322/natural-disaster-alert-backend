package com.disasteralert.common;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String,Object> health(){return Map.of("status","UP","service","disaster-alert-backend","time",Instant.now().toString());}
}
