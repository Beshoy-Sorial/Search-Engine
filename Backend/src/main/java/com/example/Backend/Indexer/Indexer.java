package com.example.Backend.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inverted_index")
public class Indexer {
    @Id
    private String token;
    private List<DocumentEntry> documents = new ArrayList<>();
    private double idf;
    public static class DocumentEntry {
        private String documentId;
        private double tf;
        private List<Integer> positions; // List<Integer>
        private List<String> sections; // Added to store sections

        public DocumentEntry() {}

        public DocumentEntry(String documentId, double tf, List<Integer> positions, List<String> sections) {
            this.documentId = documentId;
            this.tf = tf;
            this.positions = positions != null ? positions : new ArrayList<>();
            this.sections = sections != null ? sections : new ArrayList<>();
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        public double getTf() { return tf; }
        public void setTf(double tf) { this.tf = tf; }
        public List<Integer> getPositions() { return positions; }
        public void setPositions(List<Integer> positions) { this.positions = positions; }
        public List<String> getSections() { return sections; }
        public void setSections(List<String> sections) { this.sections = sections; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DocumentEntry that = (DocumentEntry) o;
            return Objects.equals(documentId, that.documentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId);
        }
    }

    public Indexer() {}

    public Indexer(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        this.token = token;
    }

    public Indexer(String token, List<DocumentEntry> documents) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        this.token = token;
        this.documents = documents != null ? documents : new ArrayList<>();
    }
    public double getIdf() { return idf; }
    public void setIdf(double idf) { this.idf = idf; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public List<DocumentEntry> getDocuments() { return documents; }
    public void setDocuments(List<DocumentEntry> documents) { this.documents = documents; }
}