package com.friday.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StagingArea {

    private final Path projectRoot;
    // Outer key: sessionId; inner key: normalized relative path
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> sessions =
        new ConcurrentHashMap<>();

    public StagingArea(@Value("${app.project-root}") String root) {
        this.projectRoot = Path.of(root).toAbsolutePath().normalize();
    }

    // Package-private constructor for tests
    StagingArea(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    public void stage(UUID sessionId, String relativePath, String content) {
        sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(relativePath, content);
    }

    public boolean hasStagedChanges(UUID sessionId) {
        ConcurrentHashMap<String, String> files = sessions.get(sessionId);
        return files != null && !files.isEmpty();
    }

    public String getStagedContent(UUID sessionId, String relativePath) {
        ConcurrentHashMap<String, String> files = sessions.get(sessionId);
        return files == null ? null : files.get(relativePath);
    }

    public Map<String, String> getAllStaged(UUID sessionId) {
        ConcurrentHashMap<String, String> files = sessions.get(sessionId);
        return files == null ? Map.of() : Map.copyOf(files);
    }

    public void clear(UUID sessionId) {
        sessions.remove(sessionId);
    }

    public Path getProjectRoot() {
        return projectRoot;
    }
}
