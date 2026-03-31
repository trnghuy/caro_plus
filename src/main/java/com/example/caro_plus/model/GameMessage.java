package com.example.caro_plus.model;

import lombok.Data;

@Data
public class GameMessage {
    private String type;    // JOIN, LEAVE, MOVE, START
    private String content; // Nội dung tin nhắn
    private String sender;  // Username người gửi
    private String roomId;  // ID phòng
    private Integer x;      // Tọa độ X (nếu là nước đi)
    private Integer y;      // Tọa độ Y (nếu là nước đi)
}