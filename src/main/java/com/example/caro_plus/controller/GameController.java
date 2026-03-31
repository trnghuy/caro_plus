package com.example.caro_plus.controller;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.dto.GameSnapshotResponse;
import com.example.caro_plus.model.GameMessage;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.GameService;
import com.example.caro_plus.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import org.springframework.http.HttpStatus;

@Controller
public class GameController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private GameState gameState;

    @Autowired
    private GameService gameService;

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

        boolean reconnected = gameState.reconnectPlayer(roomId, username);

        model.addAttribute("roomId", roomId);
        model.addAttribute("user", customUserDetails.getUser());
        model.addAttribute("room", room);
        model.addAttribute("currentTurn", gameState.getTurn(roomId));
        model.addAttribute("playerSymbol", playerSymbol);
        model.addAttribute("opponentConnected", gameState.isOpponentConnected(roomId, username));

        if (reconnected) {
            GameMessage reconnectMessage = new GameMessage();
            reconnectMessage.setType("PLAYER_RECONNECTED");
            reconnectMessage.setRoomId(roomId.toString());
            reconnectMessage.setSender(username);
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, reconnectMessage);
        }

        return "game/game";
    }

    @PostMapping("/api/games/{roomId}/disconnect")
    @ResponseBody
    public void disconnectGame(@PathVariable Long roomId, Principal principal) {
        if (principal == null) {
            return;
        }

        boolean disconnected = gameState.disconnectPlayer(roomId, principal.getName());
        if (disconnected) {
            GameMessage disconnectMessage = new GameMessage();
            disconnectMessage.setType("PLAYER_DISCONNECTED");
            disconnectMessage.setRoomId(roomId.toString());
            disconnectMessage.setSender(principal.getName());
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, disconnectMessage);
            roomService.abandonRoomIfAllPlayersDisconnected(roomId);
        }
    }

    @GetMapping("/api/games/{roomId}/state")
    @ResponseBody
    public GameSnapshotResponse getGameState(@PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is required");
        }

        GameSnapshotResponse snapshot = gameState.getSnapshot(roomId, customUserDetails.getUsername());
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game state not found");
        }

        return snapshot;
    }

    @MessageMapping("/game.move/{roomId}")
    public void makeMove(@DestinationVariable Long roomId, GameMessage message, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.makeMove(roomId, principal.getName(), message.getX(), message.getY());
        if (response != null) {
            if ("WIN".equals(response.getType())) {
                Room room = roomService.getRoomById(roomId);
                if (room != null) {
                    gameService.finishGame(room, response.getWinner());
                }
            }
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
        }
    }

    @MessageMapping("/game.replay.request/{roomId}")
    public void requestReplay(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.requestReplay(roomId, principal.getName());
        if (response != null) {
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
        }
    }

    @MessageMapping("/game.replay.accept/{roomId}")
    public void acceptReplay(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.acceptReplay(roomId, principal.getName());
        if (response != null) {
            Room room = roomService.getRoomById(roomId);
            if (room != null) {
                gameService.createGame(room);
            }
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
        }
    }

    @MessageMapping("/game.replay.decline/{roomId}")
    public void declineReplay(@DestinationVariable Long roomId, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.declineReplay(roomId, principal.getName());
        if (response != null) {
            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
        }
    }
}
