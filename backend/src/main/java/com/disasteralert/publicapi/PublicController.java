package com.disasteralert.publicapi;
import com.disasteralert.disaster.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/public")
public class PublicController {
    private final DisasterAlertRepository repo;
    public PublicController(DisasterAlertRepository repo){this.repo=repo;}
    @GetMapping("/alerts") public List<DisasterAlert> alerts(){return repo.findByStatusOrderByCreatedAtDesc("ACTIVE");}
}
