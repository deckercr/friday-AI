package com.friday.chat;

import com.friday.tools.GitHubTools;
import com.friday.tools.StagingArea;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final GitHubTools github;
    private final StagingArea staging;

    public ReviewController(GitHubTools github, StagingArea staging) {
        this.github = github;
        this.staging = staging;
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approve(@RequestBody Map<String, String> body) {
        try {
            String prUrl = github.createPR(
                body.get("branchName"),
                body.get("title"),
                "Proposed by Friday AI — approved by user."
            );
            return ResponseEntity.ok(Map.of("prUrl", prUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> reject() {
        staging.clear();
        return ResponseEntity.noContent().build();
    }
}
