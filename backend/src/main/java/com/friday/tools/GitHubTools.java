package com.friday.tools;

import com.friday.github.GitHubApiClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GitHubTools {

    private final StagingArea staging;
    private final GitHubApiClient github;

    @Autowired
    public GitHubTools(StagingArea staging, GitHubApiClient github) {
        this.staging = staging;
        this.github = github;
    }

    // Test constructor
    GitHubTools(StagingArea staging, GitHubApiClient github, java.nio.file.Path ignored) {
        this.staging = staging;
        this.github = github;
    }

    @Tool(description = """
        Commit all staged file changes to a new branch and open a PR to dev.
        The branch name should follow the pattern: friday/<short-description>.
        Returns the GitHub PR URL.
        """)
    public String createPR(String branchName, String title, String body) {
        UUID sessionId = FileTools.getCurrentSession();
        if (sessionId == null || !staging.hasStagedChanges(sessionId)) {
            return "No staged changes to commit.";
        }
        return createPRForSession(branchName, title, body, sessionId);
    }

    public String createPRForSession(String branchName, String title, String body, UUID sessionId) {
        if (!staging.hasStagedChanges(sessionId)) {
            throw new IllegalStateException("No staged changes for session " + sessionId);
        }

        String baseSha = github.getDefaultBranchSha();
        github.createBranch(branchName, baseSha);

        Map<String, String> fileShas = new HashMap<>();
        staging.getAllStaged(sessionId).forEach((path, content) -> {
            String blobSha = github.createBlob(content);
            fileShas.put(path, blobSha);
        });

        String baseTreeSha = github.getTreeSha(baseSha);
        String newTreeSha = github.createTree(baseTreeSha, fileShas);
        String commitSha = github.createCommit(title, newTreeSha, baseSha);
        github.updateBranchRef(branchName, commitSha);

        String prUrl = github.createPullRequest(branchName, title, body);
        staging.clear(sessionId);

        return prUrl;
    }
}
