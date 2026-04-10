package com.friday.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ExecTools {

    // Whitelist: allowed command prefixes
    private static final List<String> ALLOWED_PREFIXES = List.of(
        "mvn ", "mvnw ", "./mvnw ",
        "npm ", "npx ", "node ",
        "git status", "git log", "git diff", "git branch", "git show",
        "ls ", "ls", "find ", "cat ", "echo ",
        "java ", "javac "
    );

    // Blocklist: explicit denials (checked first)
    private static final List<String> BLOCKED_PATTERNS = List.of(
        "rm ", "rmdir", "dd ", "mkfs", "shutdown", "reboot",
        "curl ", "wget ", "nc ", "netcat",
        "sudo", "su ", "chmod 777", "> /dev/",
        "git push", "git commit", "git checkout", "git reset", "git merge"
    );

    private final Path projectRoot;

    public ExecTools(@Value("${app.project-root}") String root) {
        this.projectRoot = Path.of(root).toAbsolutePath().normalize();
    }

    // Package-private constructor for tests
    ExecTools(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Tool(description = """
        Execute a shell command in the project directory.
        Allowed: mvn, npm, git (read-only), ls, find, java.
        Blocked: rm, curl, wget, git push/commit/checkout, sudo.
        """)
    public String executeCommand(String command) {
        String trimmed = command.trim();

        // Reject shell metacharacters that enable injection (chaining, pipes, redirects, subshells)
        if (trimmed.matches(".*[;&|><`$\\\\].*")) {
            return "Command blocked by security policy: shell metacharacters not allowed";
        }

        for (String blocked : BLOCKED_PATTERNS) {
            if (trimmed.contains(blocked)) {
                return "Command blocked by security policy: contains '" + blocked + "'";
            }
        }

        boolean allowed = ALLOWED_PREFIXES.stream()
            .anyMatch(p -> trimmed.startsWith(p) || trimmed.equals(p.trim()));
        if (!allowed) {
            return "Command blocked by security policy: not in allowed command list";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", trimmed);
            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                try { process.getInputStream().close(); } catch (Exception ignored) {}
                return "Command timed out after 30 seconds";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                    .limit(200)
                    .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
