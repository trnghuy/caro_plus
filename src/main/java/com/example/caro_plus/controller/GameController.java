package com.example.caro_plus.controller;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.model.GameMessage;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class GameController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private GameState gameState;

    @GetMapping("/game")
    public String game(@RequestParam Long roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Model model) {

        Room room = roomService.getRoomById(roomId);
        if (room == null || !"playing".equals(room.getStatus())) {
            return "redirect:/home";
        }

        if (room.getPlayer2() == null) {
            return "redirect:/home";
        }

        String username = customUserDetails.getUsername();
        if (!gameState.hasRoom(roomId)) {
            gameState.initializeRoom(roomId, room.getHost().getUsername(), room.getPlayer2().getUsername());
        }

        String playerSymbol = gameState.getPlayerSymbol(roomId, username);
        if (playerSymbol == null) {
            return "redirect:/home";
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("user", customUserDetails.getUser());
        model.addAttribute("room", room);
        model.addAttribute("currentTurn", gameState.getTurn(roomId));
        model.addAttribute("playerSymbol", playerSymbol);

        return "game/game";
    }

    @MessageMapping("/game.move/{roomId}")
    public void makeMove(@DestinationVariable Long roomId, GameMessage message, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.makeMove(roomId, principal.getName(), message.getX(), message.getY());
        if (response != null) {
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
        }
    }
}
