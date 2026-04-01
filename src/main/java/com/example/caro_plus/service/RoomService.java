package com.example.caro_plus.service;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.model.GameMessage;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.RoomPlayer;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.RoomPlayerRepository;
import com.example.caro_plus.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private GameState gameState;

    @Autowired
    private GameService gameService;

    public Room createRoom(User user) {
        if (roomPlayerRepository.existsActiveRoomByUser(user)) {
            throw new RuntimeException("Bạn đang ở trong một phòng rồi!");
        }

        Room room = new Room();
        room.setHost(user);
        room.setCreatedAt(new Date());
        room.setStatus("waiting");

        Room savedRoom = roomRepository.save(room);

        if (roomPlayerRepository.findByRoomAndPlayer(savedRoom, user).isEmpty()) {
            RoomPlayer rp = new RoomPlayer();
            rp.setRoom(savedRoom);
            rp.setPlayer(user);
            rp.setSymbol('X');
            roomPlayerRepository.save(rp);
        }

        return savedRoom;
    }

    @Transactional
    public Room joinRoom(Long roomId, User user) {
        Room room = normalizeRoomState(roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại")));

        if (room.getHost() == null) {
            room.setHost(user);
            room.setStatus("waiting");

            RoomPlayer rp = new RoomPlayer();
            rp.setRoom(room);
            rp.setPlayer(user);
            rp.setSymbol('X');
            roomPlayerRepository.save(rp);

            return roomRepository.save(room);
        }

        if (room.getHost().getId().equals(user.getId())) {
            return room;
        }

        if (roomPlayerRepository.findByRoomAndPlayer(room, user).isPresent()) {
            return room;
        }

        if ("playing".equals(room.getStatus())) {
            throw new RuntimeException("Phòng đang trong trận đấu, không thể tham gia.");
        }

        if (room.getPlayer2() != null) {
            throw new RuntimeException("Phòng đã đầy!");
        }

        room.setPlayer2(user);
        room.setStatus("full");

        RoomPlayer rp = new RoomPlayer();
        rp.setRoom(room);
        rp.setPlayer(user);
        rp.setSymbol('O');
        roomPlayerRepository.save(rp);

        Room savedRoom = roomRepository.save(room);

        GameMessage msg = new GameMessage();
        msg.setType("JOIN");
        msg.setSender(user.getUsername());
        msg.setRoomId(roomId.toString());
        sendRoomEventAfterCommit(roomId, msg);

        return savedRoom;
    }

    @Transactional
    public Room leaveRoom(Long roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        boolean wasPlaying = "playing".equals(room.getStatus()) || gameState.hasRoom(roomId);
        boolean isHostLeaving = room.getHost() != null && room.getHost().getId().equals(user.getId());

        if (wasPlaying) {
            gameState.disconnectPlayer(roomId, user.getUsername());
        }

        roomPlayerRepository.findByRoomAndPlayer(room, user)
                .ifPresent(roomPlayerRepository::delete);

        int count = roomPlayerRepository.countByRoom(room);

        if (count == 0) {
            gameState.removeRoom(roomId);
            room.setHost(null);
            room.setPlayer2(null);
            room.setStatus("waiting");
            Room savedRoom = roomRepository.save(room);

            GameMessage roomMessage = new GameMessage();
            roomMessage.setType("LEAVE");
            roomMessage.setSender(user.getUsername());
            roomMessage.setRoomId(roomId.toString());
            sendRoomEventAfterCommit(roomId, roomMessage);

            if (wasPlaying) {
                GameMessage gameMessage = new GameMessage();
                gameMessage.setType("PLAYER_LEFT");
                gameMessage.setSender(user.getUsername());
                gameMessage.setRoomId(roomId.toString());
                sendGameEventAfterCommit(roomId, gameMessage);
            }

            return savedRoom;
        }

        if (wasPlaying && gameState.areAllPlayersDisconnected(roomId)) {
            List<RoomPlayer> remainingPlayers = roomPlayerRepository.findByRoom(room);
            if (!remainingPlayers.isEmpty()) {
                roomPlayerRepository.deleteAll(remainingPlayers);
            }

            room.setHost(null);
            room.setPlayer2(null);
            room.setStatus("waiting");
            Room savedRoom = roomRepository.save(room);
            gameState.removeRoom(roomId);

            GameMessage roomMessage = new GameMessage();
            roomMessage.setType("LEAVE");
            roomMessage.setSender(user.getUsername());
            roomMessage.setRoomId(roomId.toString());
            sendRoomEventAfterCommit(roomId, roomMessage);

            GameMessage gameMessage = new GameMessage();
            gameMessage.setType("PLAYER_LEFT");
            gameMessage.setSender(user.getUsername());
            gameMessage.setRoomId(roomId.toString());
            sendGameEventAfterCommit(roomId, gameMessage);

            return savedRoom;
        }

        if (isHostLeaving) {
            User newHost = room.getPlayer2();
            room.setHost(newHost);
            room.setPlayer2(null);

            roomPlayerRepository.findByRoomAndPlayer(room, newHost)
                    .ifPresent(roomPlayer -> roomPlayer.setSymbol('X'));
        } else if (room.getPlayer2() != null && room.getPlayer2().getId().equals(user.getId())) {
            room.setPlayer2(null);
        }

        room.setStatus("waiting");
        gameState.removeRoom(roomId);
        Room savedRoom = roomRepository.save(room);

        GameMessage roomMessage = new GameMessage();
        roomMessage.setType("LEAVE");
        roomMessage.setSender(user.getUsername());
        roomMessage.setRoomId(roomId.toString());
        sendRoomEventAfterCommit(roomId, roomMessage);

        if (wasPlaying) {
            GameMessage gameMessage = new GameMessage();
            gameMessage.setType("PLAYER_LEFT");
            gameMessage.setSender(user.getUsername());
            gameMessage.setRoomId(roomId.toString());
            sendGameEventAfterCommit(roomId, gameMessage);
        }

        return savedRoom;
    }

    @Transactional
    public Room startGame(Long roomId, User user) {
        Room room = normalizeRoomState(roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại")));

        if (!room.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Chỉ host mới được start game!");
        }

        if ("playing".equals(room.getStatus()) && gameState.hasRoom(roomId)) {
            throw new RuntimeException("Phòng đang trong trận đấu!");
        }

        if (room.getPlayer2() == null || roomPlayerRepository.countByRoom(room) != 2) {
            throw new RuntimeException("Chua du nguoi de bat dau!");
        }

        room.setStatus("playing");
        Room savedRoom = roomRepository.save(room);
        gameState.initializeRoom(roomId, room.getHost().getUsername(), room.getPlayer2().getUsername());
        gameService.createGame(savedRoom);

        GameMessage msg = new GameMessage();
        msg.setType("START");
        msg.setSender(user.getUsername());
        msg.setRoomId(roomId.toString());
        msg.setCurrentTurn(gameState.getTurn(roomId));
        sendRoomEventAfterCommit(roomId, msg);

        if (savedRoom != null) {
            return savedRoom;
        }

        throw new RuntimeException("Chưa đủ người để bắt đầu!");
    }

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .map(this::normalizeRoomState)
                .orElse(null);
    }

    @Transactional
    public Room syncRoomState(Long roomId) {
        return roomRepository.findById(roomId)
                .map(this::normalizeRoomState)
                .orElse(null);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::normalizeRoomState)
                .toList();
    }

    @Transactional
    public void abandonRoomIfAllPlayersDisconnected(Long roomId) {
        if (!gameState.areAllPlayersDisconnected(roomId)) {
            return;
        }

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            gameState.removeRoom(roomId);
            return;
        }

        List<RoomPlayer> roomPlayers = roomPlayerRepository.findByRoom(room);
        if (!roomPlayers.isEmpty()) {
            roomPlayerRepository.deleteAll(roomPlayers);
        }

        room.setHost(null);
        room.setPlayer2(null);
        room.setStatus("waiting");
        roomRepository.save(room);
        gameState.removeRoom(roomId);
    }

    private Room normalizeRoomState(Room room) {
        if (room == null) {
            return null;
        }

        List<RoomPlayer> roomPlayers = roomPlayerRepository.findByRoom(room);
        boolean changed = false;
        String currentStatus = room.getStatus();

        if (roomPlayers.isEmpty()) {
            if (room.getHost() != null) {
                room.setHost(null);
                changed = true;
            }
            if (room.getPlayer2() != null) {
                room.setPlayer2(null);
                changed = true;
            }
            if (!"waiting".equals(currentStatus)) {
                room.setStatus("waiting");
                changed = true;
            }
            if (gameState.hasRoom(room.getId())) {
                gameState.removeRoom(room.getId());
            }
        } else if (roomPlayers.size() == 1) {
            RoomPlayer remainingPlayer = roomPlayers.get(0);
            User remainingUser = remainingPlayer.getPlayer();

            if (remainingUser != null
                    && (room.getHost() == null || !room.getHost().getId().equals(remainingUser.getId()))) {
                room.setHost(remainingUser);
                changed = true;
            }

            if (room.getPlayer2() != null) {
                room.setPlayer2(null);
                changed = true;
            }

            if (!"waiting".equals(currentStatus)) {
                room.setStatus("waiting");
                changed = true;
            }

            if (remainingPlayer.getSymbol() == null || remainingPlayer.getSymbol() != 'X') {
                remainingPlayer.setSymbol('X');
                roomPlayerRepository.save(remainingPlayer);
            }

            if (gameState.hasRoom(room.getId())) {
                gameState.removeRoom(room.getId());
            }
        } else {
            RoomPlayer hostPlayer = roomPlayers.stream()
                    .filter(roomPlayer -> room.getHost() != null
                            && roomPlayer.getPlayer() != null
                            && room.getHost().getId().equals(roomPlayer.getPlayer().getId()))
                    .findFirst()
                    .orElse(roomPlayers.get(0));

            RoomPlayer secondPlayer = roomPlayers.stream()
                    .filter(roomPlayer -> !roomPlayer.getId().equals(hostPlayer.getId()))
                    .findFirst()
                    .orElse(null);

            if (hostPlayer.getPlayer() != null
                    && (room.getHost() == null || !room.getHost().getId().equals(hostPlayer.getPlayer().getId()))) {
                room.setHost(hostPlayer.getPlayer());
                changed = true;
            }

            User expectedPlayer2 = secondPlayer != null ? secondPlayer.getPlayer() : null;
            Long currentPlayer2Id = room.getPlayer2() != null ? room.getPlayer2().getId() : null;
            Long expectedPlayer2Id = expectedPlayer2 != null ? expectedPlayer2.getId() : null;
            if ((currentPlayer2Id == null && expectedPlayer2Id != null)
                    || (currentPlayer2Id != null && !currentPlayer2Id.equals(expectedPlayer2Id))) {
                room.setPlayer2(expectedPlayer2);
                changed = true;
            }

            if (hostPlayer.getSymbol() == null || hostPlayer.getSymbol() != 'X') {
                hostPlayer.setSymbol('X');
                roomPlayerRepository.save(hostPlayer);
            }

            if (secondPlayer != null && (secondPlayer.getSymbol() == null || secondPlayer.getSymbol() != 'O')) {
                secondPlayer.setSymbol('O');
                roomPlayerRepository.save(secondPlayer);
            }

            String expectedStatus = gameState.hasRoom(room.getId()) ? "playing" : "full";
            if (!expectedStatus.equals(currentStatus)) {
                room.setStatus(expectedStatus);
                changed = true;
            }
        }

        if (changed) {
            return roomRepository.save(room);
        }

        return room;
    }

    private void sendRoomEventAfterCommit(Long roomId, GameMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, message);
                }
            });
            return;
        }

        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    private void sendGameEventAfterCommit(Long roomId, GameMessage message) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, message);
                }
            });
            return;
        }

        simpMessagingTemplate.convertAndSend("/topic/game/" + roomId, message);
    }
}
