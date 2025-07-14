package com.example.Backend.Indexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
@Repository
public class IndexerRepositoryImpl implements IndexerRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void addDocumentEntry(String token, Indexer.DocumentEntry entry) {
        Query query = new Query(Criteria.where("_id").is(token));
        Update update = new Update()
                .push("documents", entry)
                .setOnInsert("_id", token);

        mongoTemplate.upsert(query, update, Indexer.class);
    }
   // not used but for extensible
    @Override
    public void upsertDocumentEntry(String token, String documentId, Indexer.DocumentEntry entry) {
        Query query = new Query(Criteria.where("_id").is(token));
        Update update = new Update()
                .pull("documents", Query.query(Criteria.where("documentId").is(documentId)))
                .addToSet("documents", entry);

        mongoTemplate.updateFirst(query, update, Indexer.class);
    }

    @Override
    public void removeDocumentEntry(String token, String documentId) {
        Query query = new Query(Criteria.where("_id").is(token));
        Update update = new Update().pull("documents", Query.query(Criteria.where("documentId").is(documentId)));

        mongoTemplate.updateFirst(query, update, Indexer.class);
    }
}