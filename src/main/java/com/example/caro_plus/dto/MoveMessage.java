package com.example.caro_plus.dto;

import lombok.Data;

@Data
public class MoveMessage {
    private Long roomId;
    private int x;
    private int y;
    private String player;
}