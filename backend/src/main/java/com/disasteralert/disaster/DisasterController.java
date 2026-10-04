package com.disasteralert.disaster;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/alerts")
public class DisasterController {
    private final DisasterService service;
    public DisasterController(DisasterService service){this.service=service;}
    @GetMapping public List<DisasterAlert> all(){return service.all();}
    @GetMapping("/active") public List<DisasterAlert> active(){return service.active();}
    @PostMapping public DisasterAlert create(@Valid @RequestBody CreateAlertRequest r){return service.create(r);}
    @PostMapping("/{id}/resolve") public DisasterAlert resolve(@PathVariable Long id){return service.close(id);}
}
