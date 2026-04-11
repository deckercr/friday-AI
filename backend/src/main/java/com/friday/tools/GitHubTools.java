package com.friday.tools;

import com.friday.github.GitHubApiClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class GitHubTools {

    private final StagingArea staging;
    private final GitHubApiClient github;
    private final Path projectRoot;

    public GitHubTools(StagingArea staging, GitHubApiClient github,
                       @Value("${app.project-root}") String root) {
        this.staging = staging;
        this.github = github;
        this.projectRoot = Path.of(root);
    }

    // Test constructor
    GitHubTools(StagingArea staging, GitHubApiClient github, Path projectRoot) {
        this.staging = staging;
        this.github = github;
        this.projectRoot = projectRoot;
    }

    @Tool(description = """
        Commit all staged file changes to a new branch and open a PR to dev.
        The branch name should follow the pattern: friday/<short-description>.
        Returns the GitHub PR URL.
        """)
    public String createPR(String branchName, String title, String body) {
        if (!staging.hasStagedChanges()) {
            return "No staged changes to commit.";
        }

        String baseSha = github.getDefaultBranchSha();
        github.createBranch(branchName, baseSha);

        Map<String, String> fileShas = new HashMap<>();
        staging.getAllStaged().forEach((path, content) -> {
            String blobSha = github.createBlob(content);
            fileShas.put(path, blobSha);
        });

        String baseTreeSha = github.getTreeSha(baseSha);
        String newTreeSha = github.createTree(baseTreeSha, fileShas);
        String commitSha = github.createCommit(title, newTreeSha, baseSha);
        github.updateBranchRef(branchName, commitSha);

        String prUrl = github.createPullRequest(branchName, title, body);
        staging.clear();

        return prUrl;
    }
}
