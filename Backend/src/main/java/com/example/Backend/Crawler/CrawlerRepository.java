package com.example.Backend.Crawler;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerRepository extends MongoRepository<Crawler, String> {
    boolean existsByUrl(String url);
    @Query(value = "{ status: ?0 }", fields = "{ _id: 1 }")
    List<Crawler> findUrlsByStatus(String status);
    @Query(value = "{ 'status': ?0 }", sort = "{ '_id': 1 }")
    List<Crawler> findByStatus(String status, Pageable pageable);
    @Query(value = "{}",fields = "{ _id: 1 }")
    List<Crawler> findAllUrls();
    @Query(value = "{ 'status': 'Indexed' }", fields = "{ _id: 1 }")
    List<Crawler> findIndexedUrls();
    @Query(value = "{ 'status': 'Crawled' }", count = true)
    int countCrawledPages();
    @Query(value = "{ 'status': 'Indexed' }", count = true)
    int countIndexedPages();
    @Query("{ 'url' : ?0 }")
    @Update("{ '$set' : { 'status' : ?1 } }")
    void setPageStatus(String url, String status);
    void deleteAllByStatus(String status);
    void deleteByUrl(String url);
    @Query("{ 'status' : 'ProcessinginIndexer' }")
    @Update("{ '$set' : { 'status' : 'Crawled' } }")
    void updateStatus();
    @Query("{ 'status' : ?0 }")
    @Update("{ '$set' : { 'status' : ?1 } }")
    void updatePageStatus(String old_status,String new_status);
    @Query(value = "{ '_id' : ?0 }",fields = " { 'outlinks' : 1 , '_id' : 1 } ")
    List<Crawler> GetOutGoingLinksByUrl(String url);
    @Query(value = " {} " ,fields = " { 'outlinks' : 1 , '_id' : 1 } ")
    List<Crawler> GetOutGoingLinks();
    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : { 'popularity' : ?1 } }")
    void updatePageRank(String url,double popularity);
    @Query(value = "{ '_id' : ?0 }",fields = " { 'popularity' :  1 } ")
    Optional<Crawler> GetPageRank(String url);
    @Query(value = "{ '_id' : ?0 }",fields = " { 'title' :  1 } ")
    Optional<Crawler> GetPageTitle(String url);
    @Query(value = "{ '_id' : ?0 }",fields = " { 'html' :  1 } ")
    Optional<Crawler> GetPageHtml(String url);
} 