package com.example.Backend.Indexer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface IndexerRepository extends MongoRepository<Indexer, String>,IndexerRepositoryCustom {
    @Query("{ 'token' : ?0 }")
    Optional<Indexer> findByToken(String token);

    @Query(value = "{ 'token' : { $in: ?0 } }", sort = "{ 'token' : 1 }")
    List<Indexer> findByTokens(List<String> tokens);

}