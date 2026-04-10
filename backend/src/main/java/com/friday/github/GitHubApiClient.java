package com.friday.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class GitHubApiClient {

    private final RestClient restClient;
    private final String repo;
    private final String baseBranch;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubApiClient(
        @Value("${app.github.token}") String token,
        @Value("${app.github.repo}") String repo,
        @Value("${app.github.base-branch}") String baseBranch,
        @Value("${app.github.api-url:https://api.github.com}") String apiUrl
    ) {
        this.repo = repo;
        this.baseBranch = baseBranch;
        this.restClient = RestClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("Authorization", "Bearer " + token)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
    }

    public String getDefaultBranchSha() {
        JsonNode node = restClient.get()
            .uri("/repos/" + repo + "/git/ref/heads/" + baseBranch)
            .retrieve().body(JsonNode.class);
        return node.path("object").path("sha").asText();
    }

    public void createBranch(String branchName, String sha) {
        ObjectNode body = mapper.createObjectNode()
            .put("ref", "refs/heads/" + branchName)
            .put("sha", sha);
        restClient.post()
            .uri("/repos/" + repo + "/git/refs")
            .body(body)
            .retrieve().toBodilessEntity();
    }

    public String createBlob(String content) {
        ObjectNode body = mapper.createObjectNode()
            .put("content", content)
            .put("encoding", "utf-8");
        JsonNode node = restClient.post()
            .uri("/repos/" + repo + "/git/blobs")
            .body(body)
            .retrieve().body(JsonNode.class);
        return node.path("sha").asText();
    }

    public String getTreeSha(String commitSha) {
        JsonNode node = restClient.get()
            .uri("/repos/" + repo + "/git/trees/" + commitSha)
            .retrieve().body(JsonNode.class);
        return node.path("sha").asText();
    }

    public String createTree(String baseTreeSha, Map<String, String> files) {
        ArrayNode treeArray = mapper.createArrayNode();
        files.forEach((path, blobSha) -> {
            ObjectNode entry = mapper.createObjectNode()
                .put("path", path)
                .put("mode", "100644")
                .put("type", "blob")
                .put("sha", blobSha);
            treeArray.add(entry);
        });
        ObjectNode body = mapper.createObjectNode()
            .put("base_tree", baseTreeSha);
        body.set("tree", treeArray);
        JsonNode node = restClient.post()
            .uri("/repos/" + repo + "/git/trees")
            .body(body)
            .retrieve().body(JsonNode.class);
        return node.path("sha").asText();
    }

    public String createCommit(String message, String treeSha, String parentSha) {
        ArrayNode parents = mapper.createArrayNode().add(parentSha);
        ObjectNode body = mapper.createObjectNode()
            .put("message", message)
            .put("tree", treeSha);
        body.set("parents", parents);
        JsonNode node = restClient.post()
            .uri("/repos/" + repo + "/git/commits")
            .body(body)
            .retrieve().body(JsonNode.class);
        return node.path("sha").asText();
    }

    public void updateBranchRef(String branchName, String commitSha) {
        ObjectNode body = mapper.createObjectNode()
            .put("sha", commitSha)
            .put("force", false);
        restClient.patch()
            .uri("/repos/" + repo + "/git/refs/heads/" + branchName)
            .body(body)
            .retrieve().toBodilessEntity();
    }

    public String createPullRequest(String head, String title, String body) {
        ObjectNode prBody = mapper.createObjectNode()
            .put("title", title)
            .put("body", body)
            .put("head", head)
            .put("base", baseBranch);
        JsonNode node = restClient.post()
            .uri("/repos/" + repo + "/pulls")
            .body(prBody)
            .retrieve().body(JsonNode.class);
        return node.path("html_url").asText();
    }
}
