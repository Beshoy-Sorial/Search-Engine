package com.example.Backend.Indexer;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indexer")
public class IndexerController {
    private final IndexerService indexerService;

    public IndexerController(IndexerService indexerService) {
        this.indexerService = indexerService;
    }

    @GetMapping
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("Search Engine Indexer API");
    }

    @PostMapping("/start")
    public ResponseEntity<String> startIndexing() {
        try {
            indexerService.startIndexing();
            return ResponseEntity.ok("Indexing process Ended successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to end indexing: " + e.getMessage());
        }
    }


    public Optional<Indexer> GetAllPagesByToken(String token) {
        return indexerService.GetAllPagesByToken(token);
    }
}