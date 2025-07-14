package com.example.Backend.SeedUrls;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "seed_urls")
public class SeedUrl {
 
    @Id
    @Field("_id")
    private String url; 

    public SeedUrl() {}

    public SeedUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
