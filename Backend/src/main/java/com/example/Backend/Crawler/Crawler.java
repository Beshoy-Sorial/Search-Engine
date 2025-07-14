package com.example.Backend.Crawler;



import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;



@Document(collection = "crawled_pages")
public class Crawler {
    
    @Id
    @Field("_id")
    private String url;

    @Field("title")
    private String title;

    @Field("html")
    private String html;


    @Field("status")
    private String status;

    @Field("outlinks")
    private List<String> outlinks;

    @Field("popularity")
    private double popularity;


    public Crawler(String url, String title, String html,String status,List<String> outlinks,double popularity) {
        this.url = url;
        this.title = title;
        this.html = html;
        this.status = status;
        this.outlinks = outlinks;
        this.popularity = popularity;
    }

    public Crawler() {
    }

    public Crawler(String url, List<String> outlinks) {
        this.url = url;
        this.outlinks = outlinks;
    }

    public Crawler(String url) {
        this.url  = url;
    }


    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public List<String> GetOutLinks() { return outlinks; }
    double GetRank() { return popularity; }
}
