package com.friday.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationMemoryService {

    private final VectorStore vectorStore;

    public ConversationMemoryService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void save(String sessionId, String role, String content) {
        var doc = new Document(
            content,
            Map.of("sessionId", sessionId, "role", role)
        );
        vectorStore.add(List.of(doc));
    }

    public List<String> searchSimilar(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build())
            .stream()
            .map(Document::getFormattedContent)
            .collect(Collectors.toList());
    }
}
