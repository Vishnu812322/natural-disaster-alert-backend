package com.disasteralert.sources;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceController {
    private final SourceService service;

    public SourceController(SourceService service) {
        this.service = service;
    }

    @GetMapping
    public List<SourceStatus> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    public SourceStatus get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/sync")
    public SourceStatus sync(@PathVariable Long id) {
        return service.sync(id);
    }

    @PatchMapping("/{id}/enabled")
    public SourceStatus enabled(@PathVariable Long id, @RequestParam boolean value) {
        return service.setEnabled(id, value);
    }
}
