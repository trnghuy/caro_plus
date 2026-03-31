package com.example.caro_plus.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    // Temporary fallback when websocket principal is unavailable.
    private Long senderId;
    private String content;
}
