package com.disasteralert.cap;

import com.disasteralert.sources.FeedSourceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CapPollingScheduler {
    private final FeedSourceRepository sourceRepository;
    private final CapIngestionService ingestionService;

    public CapPollingScheduler(
            FeedSourceRepository sourceRepository,
            CapIngestionService ingestionService) {
        this.sourceRepository = sourceRepository;
        this.ingestionService = ingestionService;
    }

    @Scheduled(fixedDelayString = "${app.cap.poll-ms:300000}", initialDelayString = "${app.cap.initial-delay-ms:15000}")
    public void poll() {
        sourceRepository.findAll().stream()
                .filter(source -> source.isEnabled() && "CAP_RSS".equals(source.getType()))
                .forEach(ingestionService::syncSource);
    }
}
