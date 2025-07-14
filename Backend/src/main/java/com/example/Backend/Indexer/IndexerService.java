package com.example.Backend.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.Backend.Crawler.Crawler;
import com.example.Backend.Crawler.CrawlerController;
import com.example.Backend.Indexer.Indexer.DocumentEntry;
import com.example.Backend.Tokenizer.Tokenizer;

@Service
public class IndexerService {
    private static final Logger logger = LoggerFactory.getLogger(IndexerService.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // Adjust thread count as needed

    private final CrawlerController crawlerController;
    private final IndexerRepository indexerRepository;
    private final Tokenizer tokenizer;


    
    public IndexerService(CrawlerController crawlerController,
                          IndexerRepository indexerRepository,
                          Tokenizer tokenizer) {
        this.crawlerController = crawlerController;
        this.indexerRepository = indexerRepository;
        this.tokenizer = tokenizer;
    }

    public void startIndexing() {
        logger.info("Indexer started");
        crawlerController.updateStatus();
        Crawler page;
        while ((page = crawlerController.GetCrawledPage()) != null) {
            Crawler finalPage = page;
            crawlerController.setPageStatus(page.getUrl(), "ProcessinginIndexer");
            executor.submit(() -> {
                try {
                    processPage(finalPage);
                } catch (Exception e) {
                    logger.error("Error in thread while indexing: {}", e.getMessage(), e);
                }
            });
        }

        executor.shutdown();
        try {
            if (executor.awaitTermination(1, TimeUnit.HOURS)) {
                logger.info("All pages indexed. Calculating IDF...");
        
                int totalDocs = crawlerController.GetIndexedPagesCount();
                List<Indexer> allTokens = indexerRepository.findAll();
        
                for (Indexer tokenEntry : allTokens) {
                    int df = tokenEntry.getDocuments().size();
                    if (df == 0) continue;
        
                    double idf = Math.log((double) totalDocs / df);
                    tokenEntry.setIdf(idf);  
                }
        
                indexerRepository.saveAll(allTokens);  // Save all with updated IDF
                logger.info("All IDF values saved.");
            } else {
                logger.warn("Executor did not terminate in time.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Indexer finished all pages");
    }

    private void processPage(Crawler page) {
        String url = page.getUrl();
        String html = page.getHtml();

        logger.info("Processing URL: {}", url);

        if (html == null || html.isBlank()) {
            logger.warn("Empty HTML for {}, marking Failed", url);
            crawlerController.setPageStatus(url, "Failed");
            return;
        }

        try {
            Document doc = Jsoup.parse(html, url);
            Map<String, TokenData> tokenMap = new ConcurrentHashMap<>();

            String title = doc.title();
            processSection(title, "title", tokenMap, url);

            String metaDescription = doc.select("meta[name=description]").attr("content");
            if (!metaDescription.isBlank()) {
                processSection(metaDescription, "meta", tokenMap, url);
            }

            processHeaders(doc, tokenMap, url);

            String bodyText = doc.body().text();
            if (bodyText.isBlank()) {
                bodyText = doc.select("p, div, article").text();
            }
            processSection(bodyText, "body", tokenMap, url);

            if (tokenMap.isEmpty()) {
                logger.warn("No tokens extracted for {}. Title: '{}', Body sample: '{}'",
                        url, title, bodyText.length() > 50 ? bodyText.substring(0, 50) : bodyText);
                crawlerController.setPageStatus(url, "Failed");
                return;
            }

            int totalTerms = tokenMap.values().stream()
                    .mapToInt(td -> td.positions.size())
                    .sum();
            logger.debug("{} unique tokens, {} total terms in {}", tokenMap.size(), totalTerms, url);

            for (Map.Entry<String, TokenData> mapEntry : tokenMap.entrySet()) {
                String token = mapEntry.getKey();
                if (token == null || token.trim().isEmpty()) {
                    logger.error("Invalid token detected in document: {}. Skipping token.", url);
                    continue;
                }
                TokenData data = mapEntry.getValue();

                double tf = (double) data.positions.size() / totalTerms;
                List<String> sections = new ArrayList<>(data.sections);
                List<Integer> positions = data.positions;

                DocumentEntry entry = new DocumentEntry(url, tf, positions, sections);
                indexerRepository.addDocumentEntry(token, entry);
                
            }

            logger.info("Indexed {} ({} tokens)", url, tokenMap.size());
            crawlerController.setPageStatus(url, "Indexed");

        } catch (Exception e) {
            logger.error("Error indexing {}: {}", url, e.getMessage(), e);
            crawlerController.setPageStatus(url, "Indexed");
        }
    }

    private void processHeaders(Document doc, Map<String, TokenData> tokenMap, String url) {
        Elements headers = doc.select("h1, h2");
        for (Element header : headers) {
            String headerText = header.text();
            if (!headerText.isBlank()) {
                processSection(headerText, "header", tokenMap, url);
            }
        }
    }

    private void processSection(String text, String sectionType, Map<String, TokenData> tokenMap, String url) {
        if (text == null || text.isBlank()) {
            logger.debug("No text to process for section '{}' in URL '{}'", sectionType, url);
            return;
        }
        logger.debug("Processing section '{}' for URL '{}': Text length = {}", sectionType, url, text.length());
        List<Tokenizer.TokenPosition> tokenPositions = tokenizer.tokenize(text, url);
        logger.debug("Extracted {} tokens from section '{}': {}", tokenPositions.size(), sectionType, tokenPositions);
        for (Tokenizer.TokenPosition tp : tokenPositions) {
            if (tp.token() != null && !tp.token().isBlank()) {
                tokenMap.computeIfAbsent(tp.token(), k -> new TokenData())
                        .add(sectionType, tp.position());
            }
        }
    }

    private static class TokenData {
        final List<Integer> positions = new CopyOnWriteArrayList<>();
        final Set<String> sections = ConcurrentHashMap.newKeySet();

        void add(String section, int position) {
            positions.add(position);
            sections.add(section);
        }
    }


    //for Query Processor
    Optional<Indexer> GetAllPagesByToken(String token) {
        return indexerRepository.findById(token); 
    }

}