package com.disasteralert.sources;

import com.disasteralert.cap.CapIngestionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SourceService {
    private final FeedSourceRepository repository;
    private final CapIngestionService ingestionService;

    public SourceService(FeedSourceRepository repository, CapIngestionService ingestionService) {
        this.repository = repository;
        this.ingestionService = ingestionService;
    }

    public List<SourceStatus> all() {
        return repository.findAll().stream().map(SourceStatus::of).toList();
    }

    public SourceStatus get(Long id) {
        return SourceStatus.of(repository.findById(id).orElseThrow());
    }

    public SourceStatus sync(Long id) {
        FeedSource source = repository.findById(id).orElseThrow();
        ingestionService.syncSource(source);
        return SourceStatus.of(repository.findById(id).orElseThrow());
    }

    public SourceStatus setEnabled(Long id, boolean enabled) {
        FeedSource source = repository.findById(id).orElseThrow();
        source.setEnabled(enabled);
        source.setUpdatedAt(Instant.now());
        return SourceStatus.of(repository.save(source));
    }
}
