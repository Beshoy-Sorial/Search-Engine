package com.example.Backend.QueryProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class Tester {
    

    private final QueryProcessor qp;

    Tester(QueryProcessor qp) {
        this.qp = qp;
    }

    @GetMapping("/hi")
    List<String> TestFunc1() 
    {
        List<String> Res = new ArrayList<>();
        qp.SearchQuery("American Cheese");
        for (String val : qp.ResultedUrls.keySet()) {
            Res.add(val);
        }
        return Res;
    }
}
