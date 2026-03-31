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

    // tạo phòng
    public Room createRoom(User user) {

        if (roomPlayerRepository.existsActiveRoomByUser(user)) {
            throw new RuntimeException("Bạn đang ở trong một phòng rồi!");
        }

        Room room = new Room();
        room.setHost(user);
        room.setCreatedAt(new Date());
        room.setStatus("waiting");

        Room savedRoom = roomRepository.save(room);

        // tránh duplicate user
        if (roomPlayerRepository.findByRoomAndPlayer(savedRoom, user).isEmpty()) {

            RoomPlayer rp = new RoomPlayer();
            rp.setRoom(savedRoom);
            rp.setPlayer(user);
            rp.setSymbol('X');

            roomPlayerRepository.save(rp);
        }

        return savedRoom;
    }

    // join phòng
    @Transactional // Rất quan trọng để dữ liệu đồng bộ ngay
    public Room joinRoom(Long roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        // 1. Nếu user đã là Host thì không cần join nữa
        if (room.getHost().getId().equals(user.getId())) {
            return room;
        }

        // 2. Kiểm tra nếu user đã có trong bảng RoomPlayer (đã join rồi)
        if (roomPlayerRepository.findByRoomAndPlayer(room, user).isPresent()) {
            return room;
        }

        // 3. Kiểm tra xem phòng đã đủ 2 người chưa (dựa trên cột player2 của bảng Room)
        if (room.getPlayer2() != null) {
            throw new RuntimeException("Phòng đã đầy!");
        }

        // --- BẮT ĐẦU CẬP NHẬT ---

        // A. Cập nhật trực tiếp vào bảng Room (Để hiển thị nhanh)
        room.setPlayer2(user);
        room.setStatus("full");

        // B. Lưu vào bảng trung gian RoomPlayer (Để quản lý Symbol 'O')
        RoomPlayer rp = new RoomPlayer();
        rp.setRoom(room);
        rp.setPlayer(user);
        rp.setSymbol('O');
        roomPlayerRepository.save(rp);

        // C. Lưu lại bảng Room
        Room savedRoom = roomRepository.save(room);

        // Gửi thông báo qua WebSocket
        GameMessage msg = new GameMessage();
        msg.setType("JOIN");
        msg.setSender(user.getUsername());
        msg.setRoomId(roomId.toString());
        sendRoomEventAfterCommit(roomId, msg);

        return savedRoom;
    }

    // rời phòng
    @Transactional
    public Room leaveRoom(Long roomId, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        if (room.getHost().getId().equals(user.getId())) {
            roomPlayerRepository.deleteAll(roomPlayerRepository.findByRoom(room));
            roomRepository.delete(room);
            gameState.removeRoom(roomId);
            return null;
        }

        roomPlayerRepository.findByRoomAndPlayer(room, user)
                .ifPresent(roomPlayerRepository::delete);

        int count = roomPlayerRepository.countByRoom(room);

        if (count == 0) {
            gameState.removeRoom(roomId);
            roomRepository.delete(room);
            return null;
        } else {
            if (room.getPlayer2() != null && room.getPlayer2().getId().equals(user.getId())) {
                room.setPlayer2(null);
            }
            room.setStatus("waiting");
            gameState.removeRoom(roomId);
            Room savedRoom = roomRepository.save(room);

            // Gửi thông báo qua WebSocket
        GameMessage msg = new GameMessage();
            msg.setType("LEAVE");
            msg.setSender(user.getUsername());
            msg.setRoomId(roomId.toString());
            sendRoomEventAfterCommit(roomId, msg);

            return savedRoom;
        }
    }

    // start game
    @Transactional
    public Room startGame(Long roomId, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        // ❗ check có phải host không
        if (!room.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Chỉ host mới được start game!");
        }

        // ❗ check đủ 2 người
        if (room.getPlayer2() == null || roomPlayerRepository.countByRoom(room) != 2) {
            throw new RuntimeException("Chua du nguoi de bat dau!");
        }

        room.setStatus("playing");
        Room savedRoom = roomRepository.save(room);
        gameState.initializeRoom(roomId, room.getHost().getUsername(), room.getPlayer2().getUsername());

            // Gửi thông báo qua WebSocket
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
}
