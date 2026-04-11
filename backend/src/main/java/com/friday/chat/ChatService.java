package com.friday.chat;

import com.friday.auth.UserRepository;
import com.friday.tools.ExecTools;
import com.friday.tools.FileTools;
import com.friday.tools.GitHubTools;
import com.friday.tools.StagingArea;
import com.friday.vector.ConversationMemoryService;
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
    private final ConversationMemoryService memoryService;

    public ChatService(ChatClient chatClient, ChatSessionRepository sessions,
                       MessageRepository messages, UserRepository users,
                       SimpMessagingTemplate ws, FileTools fileTools,
                       ExecTools execTools, GitHubTools githubTools,
                       TtsService ttsService, StagingArea staging,
                       ConversationMemoryService memoryService) {
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
        this.memoryService = memoryService;
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
    public List<Message> getMessages(String username, UUID sessionId) {
        var session = sessions.findById(sessionId).orElseThrow();
        if (!session.getUser().getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("Session not found");
        }
        return messages.findBySessionOrderByCreatedAtAsc(session);
    }

    public void streamResponse(String username, UUID sessionId, String userContent) {
        ChatSession session = loadSession(sessionId);

        saveUserMessage(session, userContent);
        memoryService.save(sessionId.toString(), "user", userContent);

        List<org.springframework.ai.chat.messages.Message> history = buildHistory(session);

        StringBuffer full = new StringBuffer();
        StringBuffer sentenceBuffer = new StringBuffer();

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

        saveAssistantMessage(session, full.toString());
        memoryService.save(sessionId.toString(), "assistant", full.toString());

        // Signal end of stream
        ws.convertAndSend("/topic/chat/" + sessionId, "[DONE]");

        // Notify UI if AI staged file changes for review
        if (staging.hasStagedChanges()) {
            String diff = fileTools.getDiff();
            ws.convertAndSend("/topic/review/" + sessionId,
                Map.of("sessionId", sessionId.toString(), "diff", diff));
        }
    }

    @Transactional(readOnly = true)
    ChatSession loadSession(UUID sessionId) {
        return sessions.findById(sessionId).orElseThrow();
    }

    @Transactional(readOnly = true)
    List<org.springframework.ai.chat.messages.Message> buildHistory(ChatSession session) {
        return messages.findBySessionOrderByCreatedAtAsc(session).stream()
            .map(m -> m.getRole().equals("user")
                ? (org.springframework.ai.chat.messages.Message) new UserMessage(m.getContent())
                : new AssistantMessage(m.getContent()))
            .toList();
    }

    @Transactional
    Message saveUserMessage(ChatSession session, String content) {
        var msg = new Message();
        msg.setSession(session);
        msg.setRole("user");
        msg.setContent(content);
        return messages.save(msg);
    }

    @Transactional
    Message saveAssistantMessage(ChatSession session, String content) {
        var msg = new Message();
        msg.setSession(session);
        msg.setRole("assistant");
        msg.setContent(content);
        return messages.save(msg);
    }

    private int findSentenceBoundary(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!' || c == '\n') {
                return i + 1;
            }
        }
        return -1;
    }
}
