package com.example.Backend.Crawler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.Backend.RobotsChecking.RobotsChecker;




@Service
public class CrawlerService {
    
    
    private final CrawlerRepository crawlerRepository;
    private final Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentLinkedQueue<String> NextToVisit = new ConcurrentLinkedQueue<>();
    private Set<String> DiscoveredUrls = Collections.synchronizedSet(new HashSet<>());
    private final RobotsChecker robotsChecker = new RobotsChecker();

    public CrawlerService(CrawlerRepository crawlerRepository) {
        this.crawlerRepository = crawlerRepository;
        
    }

    public void startCrawling(List<String> seedUrls,int maxPages,int ThreadCount){
         
        
        LoadDiscoveredUrls();
        LoadNextToVisitUrls(seedUrls);
        
        while (crawlerRepository.countCrawledPages() < maxPages) {
            visitedUrls.clear();
            LoadVisitedUrls();
            Thread Threads[] = new Thread[ThreadCount];
            for (int i = 0;i < ThreadCount;i++) {
                Threads[i] = new Thread(() -> { Crawl(maxPages); });
            }

            for (int i = 0;i < ThreadCount;i++) {
                Threads[i].start();
            }
            
            for (int i = 0;i < ThreadCount;i++) {
                try {
                    Threads[i].join();
                } catch (InterruptedException e) {
                    i--;
                }
            }
        }
        crawlerRepository.deleteAllByStatus("Pending");
        crawlerRepository.deleteAllByStatus("Blocked");
        calculatePageRank();
    }


    private void calculatePageRank() {

        System.out.println("Calculating Pages Rank Started");
        
        List<Crawler> allPages = crawlerRepository.GetOutGoingLinks();
    
        
        
     
        Map<String, Set<String>> linkGraph = new HashMap<>();
        for (Crawler crawler : allPages) {
            linkGraph.put(crawler.getUrl(), crawler.GetOutLinks() == null ? new HashSet<>() : new HashSet<> (crawler.GetOutLinks())); 
        }
      
    
        final double dampingFactor = 0.85;
        final int iterations = 20;
        final int totalPages = linkGraph.size();
        final double initialRank = 1.0 / totalPages;
    
        
        Map<String, Double> pageRank = new HashMap<>();
        for (String url : linkGraph.keySet()) {
            pageRank.put(url, initialRank);
        }
    
        for (int i = 0; i < iterations; i++) {
            System.out.println("Iteration: " + (i + 1));
            Map<String, Double> newRank = new HashMap<>();
            for (String url : linkGraph.keySet()) {
                double inboundRankSum = 0.0;
    
                for (Map.Entry<String, Set<String>> entry : linkGraph.entrySet()) {
                    String otherUrl = entry.getKey();
                    Set<String> outlinks = entry.getValue();
    
                    if (outlinks.contains(url)) {
                        int outSize = outlinks.size();
                        if (outSize > 0) {
                            inboundRankSum += pageRank.get(otherUrl) / outSize;
                        }
                    }
                }
    
                double rank = (1 - dampingFactor) / totalPages + dampingFactor * inboundRankSum;
                newRank.put(url, rank);
            }
            pageRank = newRank;
        }
    
        for (Crawler page : allPages) {
            String url = page.getUrl();
            Double rank = pageRank.get(url);
            if (rank != null) {
                crawlerRepository.updatePageRank(url, rank);
            }
        }
        System.out.println("Calculating Pages Rank Ended");
    }
    
    public int GetIndexedPagesCount() {
        return crawlerRepository.countIndexedPages();
    }

    private void Crawl(int maxPages) {
        
        while (NextToVisit.size() > 0 && visitedUrls.size() < maxPages) {
            String ProcessedUrl = "";
            try {    
                ProcessedUrl = NextToVisit.remove();

                if (!RobostCheck(ProcessedUrl)) {
                    crawlerRepository.save(new Crawler(ProcessedUrl,"","","Blocked",new ArrayList<>(),-1));
                    System.out.println("Refused: " + ProcessedUrl);                 
                    continue;
                }

                Document doc =  Jsoup.connect(ProcessedUrl).get();
                String title = doc.title();
                String html = doc.html();
                
                List<String> Links = doc.select("a[href]").eachAttr("abs:href");
                
                
                
                crawlerRepository.save(new Crawler(ProcessedUrl,title,html,"Crawled",Links,-1));
                visitedUrls.add(ProcessedUrl);
                System.out.println(visitedUrls.size());
                
                
                

                
                for (String newUrl : Links) {
                    boolean Fillter = new URI(ProcessedUrl).getHost() != null && 
                                      new URI(newUrl).getHost() != null && 
                                      new URI(ProcessedUrl).getHost().toLowerCase().equals(
                                      new URI(newUrl).getHost().toLowerCase());   
                    String NormUrl = Normalize(newUrl);       
                    if (!DiscoveredUrls.contains(NormUrl) && Fillter && NextToVisit.size() < 100000) {
                        NextToVisit.add(NormUrl);
                        DiscoveredUrls.add(NormUrl);
                        crawlerRepository.save(new Crawler(NormUrl,"","","Pending",new ArrayList<>(),-1));
                    }
                }
            }
            catch(Exception error) {
                System.out.println("Can't fetch: " + ProcessedUrl);
                crawlerRepository.save(new Crawler(ProcessedUrl,"","","Blocked",new ArrayList<>(),-1));
                continue;
            }
        }
    }

    public Optional<Crawler> GetCrawledPage() {
        return crawlerRepository.findByStatus("Crawled",PageRequest.of(0, 1)).stream().findFirst();
    }

    private boolean RobostCheck(String url) {
        return robotsChecker.isAllowed(url);
    }
    
    private void LoadVisitedUrls() {
        List<Crawler> crawlers = crawlerRepository.findUrlsByStatus("Crawled");
        for (Crawler crawler : crawlers) {
            visitedUrls.add(crawler.getUrl());
        }
        crawlers = crawlerRepository.findUrlsByStatus("Indexed");
        for (Crawler crawler : crawlers) {
            visitedUrls.add(crawler.getUrl());
        }
        crawlers = crawlerRepository.findUrlsByStatus("ProcessinginIndexer");
        for (Crawler crawler : crawlers) {
            visitedUrls.add(crawler.getUrl());
        }
    }

    private void LoadNextToVisitUrls(List<String> seedUrls) {
        List<Crawler> crawlers = crawlerRepository.findUrlsByStatus("Pending");
        if (crawlers.isEmpty()) {
            for (String url : seedUrls) {
                NextToVisit.add(Normalize(url));
                DiscoveredUrls.add(Normalize(url));
            }
        }
        else {
            for (Crawler crawler : crawlers) {
                NextToVisit.add(crawler.getUrl());
            }
        }
    }

    private String Normalize(String url) {
        try {
            URI uri = new URI(url).normalize();

            String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                
            String portPart = "";
            int port = uri.getPort();
            if (!(scheme.equals("http") && port == 80) && !(scheme.equals("https") && port == 443)) {
                portPart = port == -1 ?  "" : ":" + port;
            }

            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            if (path.toLowerCase().endsWith("/index.html")) {
                path = path.substring(0, path.length() - "/index.html".length());
            } 
                     
            return  scheme + "://" + host + portPart + path;
        } catch (Exception e) {
            return url;
        }
    } 

    private void LoadDiscoveredUrls() {
        List<Crawler> crawlers = crawlerRepository.findAllUrls();
        for (Crawler crawler : crawlers) {
            DiscoveredUrls.add(crawler.getUrl());
        }
    }

    public void setPageStatus(String url,String status) {
        crawlerRepository.setPageStatus(url, status);
    }

    public void updateStatus() {
        crawlerRepository.updateStatus();
    }

    public Optional<Crawler> GetPopularity(String Url) {
        return crawlerRepository.GetPageRank(Url);
    } 

    public Optional<Crawler> GetPageTitle(String url) {
        return crawlerRepository.GetPageTitle(url);
    }

    public Optional<Crawler> GetPageHtml(String url) {
        return crawlerRepository.GetPageHtml(url);
    }

    public List<Crawler> GetAllIndexedUrls() {
        return crawlerRepository.findIndexedUrls();
    }
}
