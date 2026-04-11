package com.friday.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);

    private final VectorStore vectorStore;

    public ConversationMemoryService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void save(String sessionId, String role, String content) {
        try {
            var doc = new Document(content, Map.of("sessionId", sessionId, "role", role));
            vectorStore.add(List.of(doc));
        } catch (Exception e) {
            log.warn("Vector store save failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    public List<String> searchSimilar(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build())
            .stream()
            .map(Document::getText)
            .collect(Collectors.toList());
    }
}
