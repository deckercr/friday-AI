package com.friday.chat;

import com.friday.tools.GitHubTools;
import com.friday.tools.StagingArea;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final GitHubTools github;
    private final StagingArea staging;
    private final ChatService chatService;

    public ReviewController(GitHubTools github, StagingArea staging, ChatService chatService) {
        this.github = github;
        this.staging = staging;
        this.chatService = chatService;
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approve(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, String> body) {
        try {
            UUID sessionId = UUID.fromString(body.get("sessionId"));
            chatService.loadSession(user.getUsername(), sessionId);  // enforces ownership, throws 403 if not owner
            String prUrl = github.createPRForSession(
                body.get("branchName"),
                body.get("title"),
                "Proposed by Friday AI — approved by user.",
                sessionId
            );
            return ResponseEntity.ok(Map.of("prUrl", prUrl));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Session not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, String> body) {
        try {
            UUID sessionId = UUID.fromString(body.get("sessionId"));
            chatService.loadSession(user.getUsername(), sessionId);  // enforces ownership, throws 403 if not owner
            staging.clear(sessionId);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception ignored) {
            // Best-effort clear — client navigates away regardless
        }
        return ResponseEntity.noContent().build();
    }
}
