package com.friday.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/stt")
public class SttController {

    private static final Logger log = LoggerFactory.getLogger(SttController.class);

    private final RestClient restClient;

    public SttController(
            @Value("${app.whisper.base-url}") String whisperUrl,
            @Value("${app.whisper.connect-timeout-seconds:5}") int connectTimeout,
            @Value("${app.whisper.read-timeout-seconds:60}") int readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        this.restClient = RestClient.builder()
            .baseUrl(whisperUrl)
            .requestFactory(factory)
            .build();
    }

    private record WhisperResponse(String text) {}

    /**
     * Proxy multipart audio to the internal Whisper service and return trimmed text.
     * Whisper prepends a leading space to transcriptions; we strip it.
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> transcribe(@RequestPart("audio") MultipartFile audio)
            throws IOException {
        var bytes = audio.getBytes();
        String originalName = audio.getOriginalFilename();
        String filename = (originalName != null && !originalName.isBlank()) ? originalName : "audio.webm";
        var resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return filename; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        WhisperResponse response;
        try {
            response = restClient.post()
                .uri("/inference")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(WhisperResponse.class);
        } catch (RestClientException | HttpMessageConversionException e) {
            log.warn("STT service unavailable: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STT service unavailable");
        }

        String text = (response != null && response.text() != null)
            ? response.text().strip() : "";
        log.debug("STT transcribed {} bytes -> {} chars", bytes.length, text.length());
        return Map.of("text", text);
    }
}
