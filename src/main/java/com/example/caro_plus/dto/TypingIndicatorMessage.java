package com.example.caro_plus.dto;

import lombok.Data;

@Data
public class TypingIndicatorMessage {
    private String senderUsername;
    private boolean typing;
}
