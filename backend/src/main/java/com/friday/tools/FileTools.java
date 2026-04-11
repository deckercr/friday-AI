package com.friday.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileTools {

    private final Path projectRoot;
    private final StagingArea staging;

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
        resolve(relativePath); // validate path before checking staging
        String staged = staging.getStagedContent(relativePath);
        if (staged != null) return staged;
        Path target = resolve(relativePath);
        try {
            return Files.readString(target);
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Stage a file write for review. The change will not be committed until the user approves.")
    public void writeFile(String relativePath, String content) {
        resolve(relativePath); // validates path — throws SecurityException on traversal
        staging.stage(relativePath, content);
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
        if (!staging.hasStagedChanges()) return "No staged changes.";
        StringBuilder diff = new StringBuilder();
        staging.getAllStaged().forEach((path, content) -> {
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
