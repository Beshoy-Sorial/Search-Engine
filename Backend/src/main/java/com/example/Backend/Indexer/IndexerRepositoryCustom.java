package com.example.Backend.Indexer;

public interface IndexerRepositoryCustom {
    void addDocumentEntry(String token, Indexer.DocumentEntry entry);
    void upsertDocumentEntry(String token, String documentId, Indexer.DocumentEntry entry);
    void removeDocumentEntry(String token, String documentId);
}