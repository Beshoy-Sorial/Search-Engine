package com.example.Backend.SeedUrls;



import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeedUrlRepository extends MongoRepository<SeedUrl, String> {
}
