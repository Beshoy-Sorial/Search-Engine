package com.example.Backend.Tokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import opennlp.tools.stemmer.PorterStemmer;

@Component
public class Tokenizer {
    private static final Logger logger = LoggerFactory.getLogger(Tokenizer.class);
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\p{L}\\p{N}']");
    private static final Pattern APOSTROPHE_PATTERN = Pattern.compile("\\b(\\w*)'(\\w*)\\b");

    private final PorterStemmer stemmer = new PorterStemmer();
    private final Set<String> stopWords;

    public Tokenizer() {
        this.stopWords = loadStopWords();
    }

    public List<TokenPosition> tokenize(String text, String documentId) {
        if (text == null) {
            logger.warn("Received null input for tokenization in document: {}", documentId);
            return new ArrayList<>();
        }

        String cleanedText = CLEAN_PATTERN.matcher(text.toLowerCase()).replaceAll(" ");
        Matcher apostropheMatcher = APOSTROPHE_PATTERN.matcher(cleanedText);
        String processedText = apostropheMatcher.replaceAll("$1$2");

        String[] words = processedText.split("\\s+");
        List<TokenPosition> tokens = new ArrayList<>();

        for (int position = 0; position < words.length; position++) {
            String word = words[position];
            if (word == null || word.isEmpty() || word.length() <= 1) {
                continue;
            }
            try {
                String stemmed = stemmer.stem(word);
                if (stopWords.contains(stemmed)) {
                    continue;
                }
                tokens.add(new TokenPosition(stemmed, position));
            } catch (Exception e) {
                logger.error("Stemming failed for token: {} in document: {}", word, documentId, e);
            }
        }

        logger.debug("Tokenized text from document '{}' into {} tokens", documentId, tokens.size());
        return tokens;
    }

    public List<TokenPosition> tokenize_keepStopWords(String text) {
        if (text == null) {
            logger.warn("Received null input for tokenization");
            return new ArrayList<>();
        }

        String cleanedText = CLEAN_PATTERN.matcher(text.toLowerCase()).replaceAll(" ");
        Matcher apostropheMatcher = APOSTROPHE_PATTERN.matcher(cleanedText);
        String processedText = apostropheMatcher.replaceAll("$1$2");

        String[] words = processedText.split("\\s+");
        List<TokenPosition> tokens = new ArrayList<>();

        for (int position = 0; position < words.length; position++) {
            String word = words[position];
            try {
                String stemmed = stemmer.stem(word);
                if (stopWords.contains(stemmed)) {
                    continue;
                }
                tokens.add(new TokenPosition(stemmed, position));
            } catch (Exception e) {
                logger.error("Stemming failed for token: {}", word);
            }
        }

        logger.debug("Tokenized text into {} tokens", tokens.size());
        return tokens;
    }

    private Set<String> loadStopWords() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource("stopwords.txt").getInputStream()))) {
                    return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(word -> {
                        try {
                            String stemmed = stemmer.stem(word);
                            return (stemmed != null && stemmed.length() > 1) ? stemmed : null;
                        } catch (Exception e) {
                            logger.warn("Failed to stem stop word: {}", word, e);
                            return null;
                        }
                    })
                    .filter(word -> word != null)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load stop words", e);
        }
    }

    public record TokenPosition(String token, int position) {
        public TokenPosition {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("Token cannot be null or blank");
            }
        }
    }
}