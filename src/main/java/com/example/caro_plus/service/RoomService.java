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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        if (room.getHost().getId().equals(user.getId())) {
            return room;
        }

        if (roomPlayerRepository.findByRoomAndPlayer(room, user).isPresent()) {
            return room;
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

        roomPlayerRepository.findByRoomAndPlayer(room, user)
                .ifPresent(roomPlayerRepository::delete);

        int count = roomPlayerRepository.countByRoom(room);

        if (count == 0) {
            gameState.removeRoom(roomId);
            roomRepository.delete(room);
            return null;
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
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        if (!room.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Chỉ host mới được start game!");
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
        return roomRepository.findById(roomId).orElse(null);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAllByOrderByCreatedAtDesc();
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
