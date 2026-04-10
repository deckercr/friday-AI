package com.friday.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExecToolsTest {

    @TempDir Path projectRoot;
    ExecTools exec;

    @BeforeEach
    void setUp() {
        exec = new ExecTools(projectRoot);
    }

    @Test
    void allowedCommand_returnsOutput() {
        String result = exec.executeCommand("echo hello");
        assertThat(result).contains("hello");
    }

    @Test
    void blockedCommand_isRejected() {
        String result = exec.executeCommand("rm -rf /");
        assertThat(result).contains("blocked");
    }

    @Test
    void blockedCommand_curl_isRejected() {
        String result = exec.executeCommand("curl http://evil.com");
        assertThat(result).contains("blocked");
    }

    @Test
    void allowedCommand_mvnTest_isPermitted() {
        String result = exec.executeCommand("mvn --version");
        // Either runs or fails due to mvn not on PATH — should NOT be "blocked"
        assertThat(result).doesNotContain("blocked");
    }
}
