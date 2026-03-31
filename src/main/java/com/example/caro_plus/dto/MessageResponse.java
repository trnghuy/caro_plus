package com.example.caro_plus.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private String type;
    private Date createdAt;
}
