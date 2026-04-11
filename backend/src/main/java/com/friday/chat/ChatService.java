package com.friday.chat;

import com.friday.auth.UserRepository;
import com.friday.tools.ExecTools;
import com.friday.tools.FileTools;
import com.friday.tools.GitHubTools;
import com.friday.tools.StagingArea;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatSessionRepository sessions;
    private final MessageRepository messages;
    private final UserRepository users;
    private final SimpMessagingTemplate ws;
    private final FileTools fileTools;
    private final ExecTools execTools;
    private final GitHubTools githubTools;
    private final TtsService ttsService;
    private final StagingArea staging;

    public ChatService(ChatClient chatClient, ChatSessionRepository sessions,
                       MessageRepository messages, UserRepository users,
                       SimpMessagingTemplate ws, FileTools fileTools,
                       ExecTools execTools, GitHubTools githubTools,
                       TtsService ttsService, StagingArea staging) {
        this.chatClient = chatClient;
        this.sessions = sessions;
        this.messages = messages;
        this.users = users;
        this.ws = ws;
        this.fileTools = fileTools;
        this.execTools = execTools;
        this.githubTools = githubTools;
        this.ttsService = ttsService;
        this.staging = staging;
    }

    @Transactional
    public ChatSession createSession(String username, String title) {
        var user = users.findByUsername(username).orElseThrow();
        var session = new ChatSession();
        session.setUser(user);
        session.setTitle(title);
        return sessions.save(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getSessionsForUser(String username) {
        var user = users.findByUsername(username).orElseThrow();
        return sessions.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
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
                .toList();

        StringBuilder full = new StringBuilder();
        StringBuilder sentenceBuffer = new StringBuilder();

        chatClient.prompt()
            .messages(history)
            .tools(fileTools, execTools, githubTools)
            .stream()
            .content()
            .doOnNext(token -> {
                full.append(token);
                sentenceBuffer.append(token);
                ws.convertAndSend("/topic/chat/" + sessionId, token);

                // Flush sentence when punctuation detected
                String buf = sentenceBuffer.toString();
                int boundary = findSentenceBoundary(buf);
                if (boundary > 0) {
                    String sentence = buf.substring(0, boundary).trim();
                    sentenceBuffer.delete(0, boundary);
                    ttsService.streamSentence(sessionId.toString(), sentence);
                }
            })
            .blockLast();

        // Flush any remaining buffer
        String remaining = sentenceBuffer.toString().trim();
        if (!remaining.isEmpty()) {
            ttsService.streamSentence(sessionId.toString(), remaining);
        }

        // Save assistant response
        var assistantMsg = new Message();
        assistantMsg.setSession(session);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(full.toString());
        messages.save(assistantMsg);

        // Signal end of stream
        ws.convertAndSend("/topic/chat/" + sessionId, "[DONE]");

        // Notify UI if AI staged file changes for review
        if (staging.hasStagedChanges()) {
            String diff = fileTools.getDiff();
            ws.convertAndSend("/topic/review/" + sessionId,
                Map.of("sessionId", sessionId.toString(), "diff", diff));
        }
    }

    private int findSentenceBoundary(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!' || c == '\n') {
                return i + 1;
            }
        }
        return -1;
    }
}
