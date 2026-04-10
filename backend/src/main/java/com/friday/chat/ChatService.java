package com.friday.chat;

import com.friday.auth.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatSessionRepository sessions;
    private final MessageRepository messages;
    private final UserRepository users;
    private final SimpMessagingTemplate ws;

    public ChatService(ChatClient chatClient, ChatSessionRepository sessions,
                       MessageRepository messages, UserRepository users,
                       SimpMessagingTemplate ws) {
        this.chatClient = chatClient;
        this.sessions = sessions;
        this.messages = messages;
        this.users = users;
        this.ws = ws;
    }

    @Transactional
    public ChatSession createSession(String username, String title) {
        var user = users.findByUsername(username).orElseThrow();
        var session = new ChatSession();
        session.setUser(user);
        session.setTitle(title);
        return sessions.save(session);
    }

    public List<ChatSession> getSessionsForUser(String username) {
        var user = users.findByUsername(username).orElseThrow();
        return sessions.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Message> getMessages(UUID sessionId) {
        var session = sessions.findById(sessionId).orElseThrow();
        return messages.findBySessionOrderByCreatedAtAsc(session);
    }

    @Transactional
    public void streamResponse(String username, UUID sessionId, String userContent) {
        var session = sessions.findById(sessionId).orElseThrow();

        // Save user message
        var userMsg = new Message();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(userContent);
        messages.save(userMsg);

        // Build conversation history for context
        List<org.springframework.ai.chat.messages.Message> history =
            messages.findBySessionOrderByCreatedAtAsc(session).stream()
                .map(m -> m.getRole().equals("user")
                    ? (org.springframework.ai.chat.messages.Message) new UserMessage(m.getContent())
                    : new AssistantMessage(m.getContent()))
                .collect(Collectors.toList());

        // Stream tokens to WebSocket, collect full response
        StringBuilder full = new StringBuilder();
        chatClient.prompt()
            .messages(history)
            .stream()
            .content()
            .doOnNext(token -> {
                full.append(token);
                ws.convertAndSend("/topic/chat/" + sessionId, token);
            })
            .blockLast();

        // Save assistant response
        var assistantMsg = new Message();
        assistantMsg.setSession(session);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(full.toString());
        messages.save(assistantMsg);

        // Signal end of stream
        ws.convertAndSend("/topic/chat/" + sessionId, "[DONE]");
    }
}
