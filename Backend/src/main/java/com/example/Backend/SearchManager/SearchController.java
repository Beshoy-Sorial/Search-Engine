package com.example.Backend.SearchManager;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Backend.Ranker.Ranker;
import com.example.Backend.Ranker.Ranker.SearchResult;

@RestController
@CrossOrigin(origins = "http://127.0.0.1:5500")
@RequestMapping("/Search")
public class SearchController {
    
    private final Ranker ranker;
    SearchController(Ranker ranker) {
        this.ranker = ranker;
    }

    @GetMapping
    public List<SearchResult> search(
        @RequestParam String query
    ) {
        return ranker.RankPages(query);
    }

}
