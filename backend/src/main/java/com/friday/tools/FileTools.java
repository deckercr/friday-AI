package com.friday.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileTools {

    // Set by ChatService before streaming so @Tool methods know their session
    private static final ThreadLocal<UUID> CURRENT_SESSION = new ThreadLocal<>();

    public static void setSession(UUID sessionId) { CURRENT_SESSION.set(sessionId); }
    public static void clearSession() { CURRENT_SESSION.remove(); }
    public static UUID getCurrentSession() { return CURRENT_SESSION.get(); }

    private final Path projectRoot;
    private final StagingArea staging;

    @Autowired
    public FileTools(@Value("${app.project-root}") String root, StagingArea staging) {
        this.projectRoot = Path.of(root).toAbsolutePath().normalize();
        this.staging = staging;
    }

    // Package-private constructor for tests
    FileTools(Path projectRoot, StagingArea staging) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.staging = staging;
    }

    @Tool(description = "Read a file from the friday-AI project. Path is relative to project root.")
    public String readFile(String relativePath) {
        Path resolved = resolve(relativePath);
        String normalizedPath = projectRoot.relativize(resolved).toString();
        UUID sessionId = CURRENT_SESSION.get();
        if (sessionId != null) {
            String staged = staging.getStagedContent(sessionId, normalizedPath);
            if (staged != null) return staged;
        }
        try {
            return Files.readString(resolved);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Stage a file write for review. The change will not be committed until the user approves.")
    public void writeFile(String relativePath, String content) {
        Path resolved = resolve(relativePath);
        String normalizedPath = projectRoot.relativize(resolved).toString();
        UUID sessionId = CURRENT_SESSION.get();
        if (sessionId == null) throw new IllegalStateException("No active session for staging");
        staging.stage(sessionId, normalizedPath, content);
    }

    @Tool(description = "List immediate children of a directory within the project (non-recursive). Path is relative to project root.")
    public String listFiles(String relativePath) {
        Path target = resolve(relativePath);
        try (Stream<Path> stream = Files.list(target)) {
            String result = stream
                .map(p -> projectRoot.relativize(p).toString())
                .sorted()
                .collect(Collectors.joining("\n"));
            return result.isEmpty() ? "(empty directory)" : result;
        } catch (IOException e) {
            return "Error listing directory: " + e.getMessage();
        }
    }

    @Tool(description = "Get a unified diff of all staged file changes pending review.")
    public String getDiff() {
        UUID sessionId = CURRENT_SESSION.get();
        if (sessionId == null || !staging.hasStagedChanges(sessionId)) return "No staged changes.";
        StringBuilder diff = new StringBuilder();
        staging.getAllStaged(sessionId).forEach((path, content) -> {
            diff.append("--- a/").append(path).append("\n");
            diff.append("+++ b/").append(path).append("\n");
            content.lines().forEach(line -> diff.append("+").append(line).append("\n"));
            diff.append("\n");
        });
        return diff.toString();
    }

    public String getDiff(UUID sessionId) {
        if (!staging.hasStagedChanges(sessionId)) return "No staged changes.";
        StringBuilder diff = new StringBuilder();
        staging.getAllStaged(sessionId).forEach((path, content) -> {
            diff.append("--- a/").append(path).append("\n");
            diff.append("+++ b/").append(path).append("\n");
            content.lines().forEach(line -> diff.append("+").append(line).append("\n"));
            diff.append("\n");
        });
        return diff.toString();
    }

    private Path resolve(String relativePath) {
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new SecurityException("Path traversal attempt blocked: " + relativePath);
        }
        return resolved;
    }
}
