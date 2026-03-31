package com.example.caro_plus.model;

import lombok.Data;

@Data
public class GameMessage {
    private String type;    // JOIN, LEAVE, MOVE, START
    private String content; // Message content
    private String sender;  // Username nguoi gui
    private String player;  // X or O for MOVE
    private String roomId;  // Room id
    private String currentTurn;
    private String winner;
    private Integer x;      // Move row
    private Integer y;      // Move column
}
