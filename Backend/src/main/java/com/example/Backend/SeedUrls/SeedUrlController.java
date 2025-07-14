package com.example.Backend.SeedUrls;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/SeedUrls")
public class SeedUrlController {
    
    private final SeedUrlService seedUrlService;

    public SeedUrlController(SeedUrlService seedUrlService) {
        this.seedUrlService = seedUrlService;
    }

    @GetMapping() 
    public List<String> SeedUrls() {
        List<String> Result = new ArrayList<String>();
        for (SeedUrl Seedurl : seedUrlService.GetStartingUrls()) {
            Result.add(Seedurl.getUrl());
        }
        return Result;
    }

}
