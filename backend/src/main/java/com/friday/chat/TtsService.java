package com.friday.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TtsService {

    private final RestClient restClient;
    private final SimpMessagingTemplate ws;

    public TtsService(@Value("${app.tts.base-url}") String ttsUrl, SimpMessagingTemplate ws) {
        this.restClient = RestClient.builder().baseUrl(ttsUrl).build();
        this.ws = ws;
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
        }
    }
}
