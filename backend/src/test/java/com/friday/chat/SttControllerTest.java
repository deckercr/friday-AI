package com.friday.chat;

import com.friday.auth.JwtService;
import com.friday.auth.UserRepository;
import com.friday.config.SecurityConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SttController.class)
@Import(SecurityConfig.class)
class SttControllerTest {

    static WireMockServer wiremock = new WireMockServer(wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        wiremock.start();
        r.add("app.whisper.base-url", wiremock::baseUrl);
    }

    @AfterAll
    static void stop() { wiremock.stop(); }

    @MockBean JwtService jwtService;
    @MockBean UserRepository userRepository;

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser
    void transcribeProxiesToWhisperAndReturnsText() throws Exception {
        wiremock.stubFor(post(urlPathEqualTo("/inference"))
            .willReturn(okJson("{\"text\": \" Hello world\"}")));

        var file = new MockMultipartFile("audio", "audio.webm",
            "audio/webm", "fake-audio".getBytes());

        mvc.perform(multipart("/api/stt/transcribe").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Hello world"));
    }

    @Test
    @WithMockUser
    void returnsEmptyTextWhenWhisperReturnsBlank() throws Exception {
        wiremock.stubFor(post(urlPathEqualTo("/inference"))
            .willReturn(okJson("{\"text\": \"\"}")));

        var file = new MockMultipartFile("audio", "audio.webm",
            "audio/webm", "fake-audio".getBytes());

        mvc.perform(multipart("/api/stt/transcribe").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value(""));
    }
}
