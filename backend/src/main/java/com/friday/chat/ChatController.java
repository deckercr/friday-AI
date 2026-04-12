package com.friday.chat;

import com.friday.chat.dto.ChatMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatMessage msg,
                               @AuthenticationPrincipal UserDetails user) {
        chatService.streamResponse(
            user.getUsername(),
            msg.sessionId(),
            msg.content()
        );
    }

    @GetMapping("/api/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
            chatService.getSessionsForUser(user.getUsername()).stream()
                .map(s -> Map.<String, Object>of(
                    "id", s.getId(),
                    "title", s.getTitle() != null ? s.getTitle() : "",
                    "createdAt", s.getCreatedAt()))
                .toList()
        );
    }

    @PostMapping("/api/sessions")
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        ChatSession s = chatService.createSession(
            user.getUsername(), body.getOrDefault("title", "New chat"));
        return ResponseEntity.ok(Map.of(
            "id", s.getId(),
            "title", s.getTitle() != null ? s.getTitle() : "",
            "createdAt", s.getCreatedAt()));
    }

    @GetMapping("/api/sessions/{id}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
            chatService.getMessages(user.getUsername(), id).stream()
                .map(m -> Map.<String, Object>of(
                    "id", m.getId(),
                    "sessionId", id,
                    "role", m.getRole(),
                    "content", m.getContent(),
                    "createdAt", m.getCreatedAt()))
                .toList()
        );
    }
}
