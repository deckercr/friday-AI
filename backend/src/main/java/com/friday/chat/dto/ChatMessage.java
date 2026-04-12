package com.friday.chat.dto;

import java.util.UUID;

public record ChatMessage(UUID sessionId, String content) {}
