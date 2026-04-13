package com.friday.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ExecTools {

    // Allowed executable names — exact match on argv[0]
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "mvn", "mvnw", "./mvnw",
        "npm", "npx",
        "git",
        "ls",
        "java", "javac"
    );

    // For git: only read-only subcommands
    private static final Set<String> ALLOWED_GIT_SUBCOMMANDS = Set.of(
        "status", "log", "diff", "branch", "show", "remote"
    );

    private final Path projectRoot;

    @Autowired
    public ExecTools(@Value("${app.project-root}") String root) {
        this.projectRoot = Path.of(root).toAbsolutePath().normalize();
    }

    // Package-private constructor for tests
    ExecTools(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Tool(description = """
        Execute a command in the project directory.
        Allowed: mvn, mvnw, npm, npx, git (read-only: status/log/diff/branch/show), ls, java, javac.
        All path arguments must reside within the project directory.
        """)
    public String executeCommand(String command) {
        String trimmed = command.trim();

        // Reject control characters — newlines/CR/null enable multi-command injection
        // even without a shell, since the string is split on whitespace below
        if (trimmed.chars().anyMatch(c -> c == '\n' || c == '\r' || c == '\0')) {
            return "Command blocked: control characters not allowed";
        }

        // Parse into argv — no shell, no quoting, no metacharacters
        List<String> argv = Arrays.stream(trimmed.split("\\s+"))
            .filter(s -> !s.isEmpty())
            .toList();

        if (argv.isEmpty()) {
            return "Command blocked: empty command";
        }

        // Reject shell metacharacters in any token — defense-in-depth in case a shell
        // is ever re-introduced, and keeps argument intent unambiguous
        boolean hasMeta = argv.stream().anyMatch(arg ->
            arg.chars().anyMatch(c -> "|&;<>`$\\".indexOf(c) >= 0));
        if (hasMeta) {
            return "Command blocked: shell metacharacters not allowed";
        }

        String cmd = argv.get(0);

        // Validate executable against allowlist
        if (!ALLOWED_COMMANDS.contains(cmd)) {
            return "Command blocked: '" + cmd + "' is not in the allowed command list";
        }

        // Validate git subcommand
        if ("git".equals(cmd)) {
            if (argv.size() < 2 || !ALLOWED_GIT_SUBCOMMANDS.contains(argv.get(1))) {
                String allowed = String.join(", ", ALLOWED_GIT_SUBCOMMANDS);
                return "Command blocked: git subcommand not allowed (permitted: " + allowed + ")";
            }
        }

        // Validate all non-flag arguments resolve within projectRoot
        for (int i = 1; i < argv.size(); i++) {
            String arg = argv.get(i);
            if (!arg.startsWith("-")) {
                Path resolved = projectRoot.resolve(arg).normalize();
                if (!resolved.startsWith(projectRoot)) {
                    return "Command blocked: path argument '" + arg + "' resolves outside project root";
                }
            }
        }

        try {
            // No shell — execute argv directly to eliminate injection surface
            ProcessBuilder pb = new ProcessBuilder(argv);
            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain stdout concurrently to prevent pipe-buffer deadlock
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    return reader.lines()
                        .limit(200)
                        .collect(Collectors.joining("\n"));
                } catch (Exception e) {
                    return "";
                }
            });

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out after 30 seconds";
            }

            return outputFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
