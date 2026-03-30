package com.example.caro_plus.config;

import java.util.*;

import org.springframework.stereotype.Component;

@Component
public class GameState {

    private Map<Long, String> currentTurn = new HashMap<>();

    public String getTurn(Long roomId) {
        return currentTurn.getOrDefault(roomId, "X");
    }

    public void switchTurn(Long roomId) {
        currentTurn.put(roomId,
                getTurn(roomId).equals("X") ? "O" : "X");
    }
}