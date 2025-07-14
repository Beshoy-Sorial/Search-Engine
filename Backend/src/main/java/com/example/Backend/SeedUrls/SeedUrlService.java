package com.example.Backend.SeedUrls;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SeedUrlService {
    
    private final SeedUrlRepository seedUrlRepository;

    public SeedUrlService(SeedUrlRepository seedUrlRepository) {
        this.seedUrlRepository = seedUrlRepository;
    }

    public List<SeedUrl> GetStartingUrls() {
        return seedUrlRepository.findAll();
    }
}
