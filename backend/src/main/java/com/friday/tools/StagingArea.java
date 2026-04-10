package com.friday.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StagingArea {

    private final Path projectRoot;
    private final Map<String, String> staged = new ConcurrentHashMap<>();

    public StagingArea(@Value("${app.project-root}") String root) {
        this.projectRoot = Path.of(root).toAbsolutePath().normalize();
    }

    // Package-private constructor for tests
    StagingArea(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    public void stage(String relativePath, String content) {
        staged.put(relativePath, content);
    }

    public boolean hasStagedChanges() {
        return !staged.isEmpty();
    }

    public String getStagedContent(String relativePath) {
        return staged.get(relativePath);
    }

    public Map<String, String> getAllStaged() {
        return Collections.unmodifiableMap(staged);
    }

    public void clear() {
        staged.clear();
    }

    public Path getProjectRoot() {
        return projectRoot;
    }
}
