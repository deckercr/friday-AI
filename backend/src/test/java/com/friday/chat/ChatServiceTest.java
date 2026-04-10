package com.friday.chat;

import com.friday.auth.User;
import com.friday.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class ChatServiceTest {

    @Autowired ChatService chatService;
    @MockBean ChatSessionRepository sessionRepo;
    @MockBean MessageRepository messageRepo;
    @MockBean UserRepository userRepo;

    @Test
    void createSession_returnsSessionWithTitle() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        ChatSession session = new ChatSession();
        session.setTitle("Test Session");
        when(sessionRepo.save(any())).thenReturn(session);

        ChatSession result = chatService.createSession("alice", "Test Session");
        assertThat(result.getTitle()).isEqualTo("Test Session");
    }
}
