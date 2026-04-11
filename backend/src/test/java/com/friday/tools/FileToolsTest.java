package com.friday.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileToolsTest {

    @TempDir Path projectRoot;
    FileTools tools;
    StagingArea staging;
    UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        staging = new StagingArea(projectRoot);
        tools = new FileTools(projectRoot, staging);
        FileTools.setSession(sessionId);
    }

    @AfterEach
    void tearDown() {
        FileTools.clearSession();
    }

    @Test
    void readFile_returnsContent() throws IOException {
        Files.writeString(projectRoot.resolve("hello.txt"), "world");
        assertThat(tools.readFile("hello.txt")).isEqualTo("world");
    }

    @Test
    void readFile_rejectsPathTraversal() {
        assertThatThrownBy(() -> tools.readFile("../../etc/passwd"))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void listFiles_returnsFilenames() throws IOException {
        Files.writeString(projectRoot.resolve("a.txt"), "a");
        Files.writeString(projectRoot.resolve("b.txt"), "b");
        String result = tools.listFiles(".");
        assertThat(result).contains("a.txt").contains("b.txt");
    }

    @Test
    void writeFile_stagesContent() {
        tools.writeFile("new.txt", "content");
        assertThat(staging.hasStagedChanges(sessionId)).isTrue();
        assertThat(staging.getStagedContent(sessionId, "new.txt")).isEqualTo("content");
    }

    @Test
    void getDiff_showsStagedChanges() {
        tools.writeFile("new.txt", "content");
        String diff = tools.getDiff();
        assertThat(diff).contains("new.txt");
    }
}
