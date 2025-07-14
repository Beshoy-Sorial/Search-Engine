package com.example.Backend.QueryProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import com.example.Backend.Crawler.Crawler;
import com.example.Backend.Crawler.CrawlerController;
import com.example.Backend.Indexer.Indexer;
import com.example.Backend.Indexer.Indexer.DocumentEntry;
import com.example.Backend.Indexer.IndexerController;
import com.example.Backend.Tokenizer.Tokenizer;
import com.example.Backend.Tokenizer.Tokenizer.TokenPosition;


@Component
public class QueryProcessor {
    
    public Map<String,PageMetaData> ResultedUrls = new HashMap<>();
   

    private final Tokenizer tokenizer;
    private final IndexerController indexerController;
    private final CrawlerController crawlerController;

    public QueryProcessor(Tokenizer tokenizer,IndexerController indexerController,CrawlerController crawlerController) {
        this.tokenizer = tokenizer;
        this.indexerController = indexerController;
        this.crawlerController = crawlerController;
    }


    public void SearchQuery(String Query) {
        if (Logic_Search(Query)) return;

        List<String> Phrases = new ArrayList<>();
        String Terms = new String();

        int i = 0;
        while (i < Query.length()) {
            if (Query.charAt(i) == '\"') {
                int phraseEnd = Query.indexOf("\"", i + 1); 
                if (phraseEnd == -1) {
                    i++;
                    continue; 
                }
                String phrase = Query.substring(i + 1, phraseEnd);
                Phrases.add(phrase);
                i = phraseEnd + 1; 
            } else {
                int nextQuote = Query.indexOf("\"", i);
                if (nextQuote == -1) {
                    nextQuote = Query.length(); // No more quotes, process remaining query
                }
                String[] words = Query.substring(i, nextQuote).trim().split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        Terms += (word + " ");
                    }
                }
                i = nextQuote; 
            }
        }
        
        List<String> WordTokens =   tokenizer.tokenize(Terms, "").stream()
                                    .map(TokenPosition::token)
                                    .collect(Collectors.toList());  
        WordSearch(WordTokens);
        PhraseSearch(Phrases);
    }

    void WordSearch(List<String> WordTokens) {
        for (String token : WordTokens) {
            Optional<Indexer> pages = indexerController.GetAllPagesByToken(token);
            if (pages.get() != null) {
                List<DocumentEntry> links = pages.get().getDocuments();
                for (DocumentEntry link : links) {
                    ResultedUrls.putIfAbsent(link.getDocumentId(),new PageMetaData());
                    ResultedUrls.get(link.getDocumentId()).Words.add(new SearchData(link.getTf(),pages.get().getIdf(),link.getPositions()));
                    ResultedUrls.get(link.getDocumentId()).rank = crawlerController.GetPagePopularity(link.getDocumentId());
                }
            }
        }
    }   
    
     
    public String Generate_Snippit(String url) {
        String html = crawlerController.GetPageHtml(url);
        if (html.equals("No Html")) return "";
        

        String text = Jsoup.parse(html).text();
        PageMetaData meta = ResultedUrls.get(url);
        
        if (meta == null || meta.Words.isEmpty()) {
            return text.substring(0, Math.min(150, text.length())) + (text.length() > 150 ? "..." : "");
        }
        

        List<Integer> allPositions = new ArrayList<>();
        for (SearchData word : meta.Words) {
            allPositions.addAll(word.Positions);
        }
        Collections.sort(allPositions);
        
        
        int snippetStart = findBestSnippetPosition(allPositions, text.length());
        String[] words = text.split("\\s+");
        int start = Math.max(0, snippetStart - 12);
        int end = Math.min(words.length, snippetStart + 13);
        

        StringBuilder snippet = new StringBuilder();
        Set<String> queryTerms = meta.Words.stream()
            .map(w -> text.substring(w.Positions.get(0), w.Positions.get(0) + 1))
            .collect(Collectors.toSet());
        
        for (int i = start; i < end; i++) {
            String word = words[i];
            if (queryTerms.contains(word)) {
                snippet.append("<b>").append(word).append("</b> ");
            } else {
                snippet.append(word).append(" ");
            }
        }
        
        return snippet.toString().trim() + (end < words.length ? "..." : "");
    }

    private int findBestSnippetPosition(List<Integer> positions, int textLength) {
        if (positions.isEmpty()) return 0;
        
      
        int windowSize = 100; 
        int maxCount = 0;
        int bestPos = positions.get(0);
        
        for (int i = 0; i < positions.size(); i++) {
            int current = positions.get(i);
            int count = 1;
            
            for (int j = i + 1; j < positions.size() && positions.get(j) <= current + windowSize; j++) {
                count++;
            }
            
            if (count > maxCount) {
                maxCount = count;
                bestPos = current;
            }
        }
        
        return bestPos;
    }

    void PhraseSearch(List<String> Phrases) {
        List<Crawler> crawlers = crawlerController.GetAllIndexedUrls();
        int totalPages = crawlers.size();
        
        for (String phrase : Phrases) {
            // Convert phrase to lowercase for case-insensitive matching
            String searchPhrase = phrase.toLowerCase();
            List<String> matchingUrls = new ArrayList<>();
            
            // First pass: find all matching documents
            for (Crawler crawler : crawlers) {
                String html = crawlerController.GetPageHtml(crawler.getUrl());
                if (html == null || html.isEmpty()) continue;
                
                // Parse HTML once
                String text = Jsoup.parse(html).text().toLowerCase();
                int wordCount = text.split("\\s+").length;
                
                if (text.contains(searchPhrase)) {
                    String url = crawler.getUrl();
                    matchingUrls.add(url);
                    
                    // Get all positions of the phrase
                    List<Integer> positions = GetPositions(text, searchPhrase);
                    
                    // Calculate TF: number of occurrences / total words in document
                    double tf = (double) positions.size() / wordCount;
                    
                    // Add to results with initial IDF of 0 (will be updated later)
                    ResultedUrls.putIfAbsent(url, new PageMetaData());
                    ResultedUrls.get(url).Words.add(new SearchData(tf * 2.0, 0, positions));
                    ResultedUrls.get(url).rank = crawlerController.GetPagePopularity(url);
                }
            }
            
            // Second pass: calculate IDF for matching documents
            if (!matchingUrls.isEmpty()) {
                double idf = Math.log((double) totalPages / matchingUrls.size());
                
                // Update IDF for all matching documents
                for (String url : matchingUrls) {
                    if (ResultedUrls.containsKey(url)) {
                        for (SearchData word : ResultedUrls.get(url).Words) {
                            word.idf = idf;
                        }
                    }
                }
            }
        }
    }
    

    private List<Integer> GetPositions(String text, String phrase) {
        List<Integer> positions = new ArrayList<>();
        int pos = 0;
        while ((pos = text.indexOf(phrase, pos)) != -1) {
            positions.add(pos);
            // Move past the current match to find next occurrence
            pos += phrase.length();
        }
        return positions;
    }

    public boolean Logic_Search(String Query) {
        return false;
    }


    public class PageMetaData {
       public Set<SearchData> Words;
       public double rank;

       public PageMetaData() {
            Words = new HashSet<>();
       }
    }

    public class SearchData {
        public double tf;
        public double idf;
        List<Integer> Positions;

        public SearchData(double tf,double idf,List<Integer> Positions) {
            this.tf = tf;
            this.idf = idf;
            this.Positions = Positions;
        }
    }
}
