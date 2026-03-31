package com.example.caro_plus.controller;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.dto.MoveMessage;
import com.example.caro_plus.service.GameService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameState gameState;

    @Autowired
    private GameService gameService;

    @MessageMapping("/move")
    public void sendMove(MoveMessage message) {

        String current = gameState.getTurn(message.getRoomId());

        if (!current.equals(message.getPlayer())) {
            return;
        }
        //
        gameState.switchTurn(message.getRoomId());

        messagingTemplate.convertAndSend(
                "/topic/game/" + message.getRoomId(),
                message);
    }
}
