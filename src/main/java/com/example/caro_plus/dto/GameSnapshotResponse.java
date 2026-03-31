package com.example.caro_plus.dto;

import lombok.Data;

@Data
public class GameSnapshotResponse {
    private String[][] board;
    private String currentTurn;
    private String winner;
    private Integer lastMoveX;
    private Integer lastMoveY;
    private boolean opponentConnected;
}
