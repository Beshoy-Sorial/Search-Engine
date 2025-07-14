package com.example.Backend.Crawler;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Backend.SeedUrls.SeedUrlController;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {
    private final CrawlerService crawlerService;
    private final SeedUrlController seedUrlController;

    public CrawlerController(CrawlerService crawlerService,SeedUrlController seedUrlController) {
        this.crawlerService = crawlerService;
        this.seedUrlController = seedUrlController;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startCrawling(@RequestParam(defaultValue = "10") int threadCount) {
        System.out.println("Crawling Starting");
        crawlerService.startCrawling(seedUrlController.SeedUrls(), 6000,threadCount);
        return ResponseEntity.ok("Crawling Ended!");
    }


    @GetMapping()
    public Crawler GetCrawledPage() {
        Optional<Crawler> page = crawlerService.GetCrawledPage();
        return page.isPresent() ? page.get() : null;
    }

    public void setPageStatus(String url,String status) {
        crawlerService.setPageStatus(url, status);
    }

    public void updateStatus() {
        crawlerService.updateStatus();
    }

    public int GetIndexedPagesCount () {
        return crawlerService.GetIndexedPagesCount();
    }

    public double GetPagePopularity(String url) {
        Crawler crawler;
        if ( (crawler = crawlerService.GetPopularity(url).get()) != null) {
            return crawler.GetRank();
        }
        return 0;
    }

    public String GetPageTitle(String url) {
        Crawler crawler;
        if ( (crawler = crawlerService.GetPageTitle(url).get()) != null) {
            return crawler.getTitle();
        }
        return "No Title";
    }


    public String GetPageHtml(String url) {
        Crawler crawler;
        if ( (crawler = crawlerService.GetPageHtml(url).get()) != null) {
            return crawler.getHtml();
        }
        return "No Html";
    }

    public List<Crawler> GetAllIndexedUrls() {
        return crawlerService.GetAllIndexedUrls();
    }
}
