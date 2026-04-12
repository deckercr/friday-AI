package com.friday.chat;

import com.friday.auth.User;
import com.friday.auth.UserRepository;
import com.friday.tools.ExecTools;
import com.friday.tools.FileTools;
import com.friday.tools.GitHubTools;
import com.friday.tools.StagingArea;
import com.friday.vector.ConversationMemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatClient chatClient;
    @Mock ChatSessionRepository sessionRepo;
    @Mock MessageRepository messageRepo;
    @Mock UserRepository userRepo;
    @Mock SimpMessagingTemplate ws;
    @Mock FileTools fileTools;
    @Mock ExecTools execTools;
    @Mock GitHubTools githubTools;
    @Mock TtsService ttsService;
    @Mock StagingArea staging;
    @Mock ConversationMemoryService memoryService;

    @InjectMocks ChatService chatService;

    @Test
    void createSession_savesSessionWithUserAndTitle() {
        User user = mock(User.class);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        ChatSession saved = new ChatSession();
        saved.setTitle("Test Session");
        when(sessionRepo.save(any())).thenReturn(saved);

        ChatSession result = chatService.createSession("alice", "Test Session");

        assertThat(result.getTitle()).isEqualTo("Test Session");

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepo).save(captor.capture());
        ChatSession persisted = captor.getValue();
        assertThat(persisted.getUser()).isSameAs(user);
        assertThat(persisted.getTitle()).isEqualTo("Test Session");
    }
}
