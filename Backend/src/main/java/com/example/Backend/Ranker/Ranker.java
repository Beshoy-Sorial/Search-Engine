package com.example.Backend.Ranker;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.Backend.Crawler.CrawlerController;
import com.example.Backend.QueryProcessor.QueryProcessor;
import com.example.Backend.QueryProcessor.QueryProcessor.PageMetaData;
import com.example.Backend.QueryProcessor.QueryProcessor.SearchData;

@Component
public class Ranker {
    

    private final QueryProcessor queryProcessor;
    private final CrawlerController crawlerController;

    public Ranker(QueryProcessor queryProcessor,CrawlerController crawlerController) {
        this.queryProcessor = queryProcessor;
        this.crawlerController = crawlerController;
    }



    public List<SearchResult> RankPages(String Query) {
        queryProcessor.SearchQuery(Query);
        List<SearchResult> Res = new ArrayList<>();
        for (String link : queryProcessor.ResultedUrls.keySet()) {
            PageMetaData page_data = queryProcessor.ResultedUrls.get(link);
            double tf_idf = 0;
            for (SearchData searchData : page_data.Words) {
                tf_idf += searchData.idf * searchData.tf;
            }
            double score = .9 * tf_idf + .1 * page_data.rank;
            Res.add(new SearchResult(link, crawlerController.GetPageTitle(link), queryProcessor.Generate_Snippit(link), score));
        }
        queryProcessor.ResultedUrls.clear();
        return Res.stream().sorted((a, b) -> Double.compare(b.score, a.score)).limit(25).collect(Collectors.toList());
    }


    public class SearchResult {
        public String url;
        public String title;
        public String snippet;  
        public double score;    
        
        
        SearchResult(String url,String title,String snippet,double score) {
            this.url = url;
            this.title = title;
            this.snippet = snippet;
            this.score = score;
        }

    }

}
