package com.example.caro_plus.controller;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.dto.GameSnapshotResponse;
import com.example.caro_plus.dto.SupportActionResponse;
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
import org.springframework.http.ResponseEntity;

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

    @PostMapping("/api/games/{roomId}/support/undo")
    @ResponseBody
    public synchronized ResponseEntity<SupportActionResponse> undoMove(@PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = customUserDetails.getUsername();
        if (!gameState.canUndoLastMove(roomId, username)) {
            return ResponseEntity.badRequest().body(buildSupportError("Hiện tại chưa thể quay lại nước đi trước."));
        }

        try {
            double remainingStars = gameService.spendSupportPoints(username, 2);
            GameMessage undoMessage = gameState.undoLastMove(roomId, username);
            if (undoMessage == null) {
                return ResponseEntity.badRequest().body(buildSupportError("Hiện tại chưa thể quay lại nước đi trước."));
            }
            Room room = roomService.getRoomById(roomId);
            if (room != null) {
                gameService.removeLastMoves(room, 2);
            }
            SupportActionResponse response = new SupportActionResponse();
            response.setMessage("Đã quay lại 2 nước gần nhất. Bạn mất 2 sao.");
            response.setSupportPoints(remainingStars);

            simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, undoMessage);
            broadcastSupportPointsUpdated(roomId, username, "undo");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(buildSupportError(exception.getMessage()));
        }
    }

    @PostMapping("/api/games/{roomId}/support/suggestion")
    @ResponseBody
    public synchronized ResponseEntity<SupportActionResponse> suggestMove(@PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = customUserDetails.getUsername();
        GameState.SuggestedMove suggestion = gameState.suggestMove(roomId, username);
        if (suggestion == null) {
            return ResponseEntity.badRequest().body(buildSupportError("Hiện tại chưa thể dùng gợi ý nước đi."));
        }

        try {
            double remainingStars = gameService.spendSupportPoints(username, 3);
            SupportActionResponse response = new SupportActionResponse();
            response.setMessage("Đã dùng gợi ý nước đi. Bạn mất 3 sao.");
            response.setSupportPoints(remainingStars);
            response.setSuggestedX(suggestion.getX());
            response.setSuggestedY(suggestion.getY());

            broadcastSupportPointsUpdated(roomId, username, "suggestion");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException exception) {
            return ResponseEntity.badRequest().body(buildSupportError(exception.getMessage()));
        }
    }

    @MessageMapping("/game.move/{roomId}")
    public void makeMove(@DestinationVariable Long roomId, GameMessage message, Principal principal) {
        if (principal == null) {
            return;
        }

        GameMessage response = gameState.makeMove(roomId, principal.getName(), message.getX(), message.getY());
        if (response != null) {
            Room room = roomService.getRoomById(roomId);
            if (room != null && response.getX() != null && response.getY() != null
                    && ("MOVE".equals(response.getType()) || "WIN".equals(response.getType()))) {
                gameService.recordMove(room, principal.getName(), response.getX(), response.getY());
            }
            if ("WIN".equals(response.getType())) {
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

    private SupportActionResponse buildSupportError(String message) {
        SupportActionResponse response = new SupportActionResponse();
        response.setMessage(message);
        return response;
    }

    private void broadcastSupportPointsUpdated(Long roomId, String username, String action) {
        GameMessage response = new GameMessage();
        response.setType("SUPPORT_POINTS_UPDATED");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setContent(action);
        simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, response);
    }
}
