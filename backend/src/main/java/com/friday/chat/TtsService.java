package com.friday.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    private final RestClient restClient;
    private final SimpMessagingTemplate ws;

    public TtsService(
            @Value("${app.tts.base-url}") String ttsUrl,
            @Value("${app.tts.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${app.tts.read-timeout-seconds:15}") int readTimeoutSeconds,
            SimpMessagingTemplate ws) {
        this.ws = ws;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.restClient = RestClient.builder()
            .baseUrl(ttsUrl)
            .requestFactory(factory)
            .build();
    }

    /**
     * Send a sentence to Qwen TTS, receive audio bytes, forward over WebSocket.
     * Sent as binary to /topic/audio/{sessionId}.
     */
    public void streamSentence(String sessionId, String sentence) {
        if (sentence.isBlank()) return;
        try {
            byte[] audio = restClient.post()
                .uri("/synthesize")
                .body(Map.of("text", sentence))
                .retrieve()
                .body(byte[].class);
            if (audio != null && audio.length > 0) {
                ws.convertAndSend("/topic/audio/" + sessionId, audio);
            }
        } catch (Exception e) {
            // TTS failure is non-fatal — chat still works, just no audio
            log.warn("TTS synthesis failed for session {} ({} chars): {}",
                sessionId, sentence.length(), e.getMessage());
        }
    }
}
