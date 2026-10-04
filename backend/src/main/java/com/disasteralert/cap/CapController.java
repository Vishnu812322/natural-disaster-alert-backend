package com.disasteralert.cap;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.disasteralert.sources.FeedSource;
import com.disasteralert.sources.FeedSourceRepository;

@RestController
@RequestMapping("/api/cap")
public class CapController {

    private final CapParser parser;
    private final CapIngestionService ingestionService;
    private final FeedSourceRepository sourceRepository;

    public CapController(
            CapParser parser,
            CapIngestionService ingestionService,
            FeedSourceRepository sourceRepository) {

        this.parser = parser;
        this.ingestionService = ingestionService;
        this.sourceRepository = sourceRepository;
    }

    @PostMapping(
            value = "/parse",
            consumes = {
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.TEXT_XML_VALUE
            }
    )
    public CapParser.ParsedCap parse(
            @RequestBody byte[] xml
    ) throws Exception {

        return parser.parse(
                new ByteArrayInputStream(xml)
        );
    }

    @PostMapping("/sync")
public String sync() {

    List<FeedSource> sources =
            sourceRepository.findAll();

    StringBuilder result = new StringBuilder();

    for (FeedSource source : sources) {

        if (!source.isEnabled()) {
            continue;
        }

        ingestionService.syncSource(source);

        result.append("Source: ")
                .append(source.getCode())
                .append("\n")
                .append("URL: ")
                .append(source.getUrl())
                .append("\n")
                .append("HTTP status: ")
                .append(source.getLastHttpStatus())
                .append("\n")
                .append("Items: ")
                .append(source.getLastItemCount())
                .append("\n")
                .append("Last error/status: ")
                .append(source.getLastError())
                .append("\n\n");
    }

    return result.length() == 0
            ? "No enabled CAP source found."
            : result.toString();
}
}