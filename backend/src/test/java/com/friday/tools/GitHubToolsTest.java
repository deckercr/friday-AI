package com.friday.tools;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.friday.github.GitHubApiClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubToolsTest {

    static WireMockServer wireMock;
    @TempDir Path projectRoot;
    GitHubTools tools;
    StagingArea staging;
    UUID sessionId;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        staging = new StagingArea(projectRoot);
        var client = new GitHubApiClient(
            "test-token", "owner/repo", "dev",
            wireMock.baseUrl()
        );
        tools = new GitHubTools(staging, client, projectRoot);
        FileTools.setSession(sessionId);
    }

    @AfterEach
    void tearDown() {
        FileTools.clearSession();
    }

    @Test
    void createPR_callsGitHubApi() {
        // Stub get default branch SHA
        wireMock.stubFor(get(urlPathMatching("/repos/owner/repo/git/ref/heads/dev"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"object":{"sha":"abc123"}}
                    """)));

        // Stub create branch
        wireMock.stubFor(post(urlPathMatching("/repos/owner/repo/git/refs"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{}")));

        // Stub create blob for each file
        wireMock.stubFor(post(urlPathMatching("/repos/owner/repo/git/blobs"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"sha":"blobsha1"}
                    """)));

        // Stub get tree
        wireMock.stubFor(get(urlPathMatching("/repos/owner/repo/git/trees/abc123"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"sha":"abc123","tree":[]}
                    """)));

        // Stub create tree
        wireMock.stubFor(post(urlPathMatching("/repos/owner/repo/git/trees"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"sha":"treesha1"}
                    """)));

        // Stub create commit
        wireMock.stubFor(post(urlPathMatching("/repos/owner/repo/git/commits"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"sha":"commitsha1"}
                    """)));

        // Stub update branch ref
        wireMock.stubFor(patch(urlPathMatching("/repos/owner/repo/git/refs/heads/friday/test-feature"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{}")));

        // Stub create PR
        wireMock.stubFor(post(urlPathMatching("/repos/owner/repo/pulls"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("""
                    {"html_url":"https://github.com/owner/repo/pull/1"}
                    """)));

        staging.stage(sessionId, "src/Test.java", "public class Test {}");

        String result = tools.createPR("friday/test-feature", "Test PR", "Test body");
        assertThat(result).contains("https://github.com/owner/repo/pull/1");
    }
}
