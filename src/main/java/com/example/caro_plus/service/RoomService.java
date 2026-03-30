package com.example.caro_plus.service;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.RoomPlayer;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.RoomPlayerRepository;
import com.example.caro_plus.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;

@Service
public class RoomService {

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private RoomRepository roomRepository;

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
    public Room joinRoom(Long roomId, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        // ❌ đã trong room
        if (roomPlayerRepository.findByRoomAndPlayer(room, user).isPresent()) {
            return room;
        }

        // ❌ full rồi
        if (roomPlayerRepository.countByRoom(room) == 2) {
            return null;
        }

        // thêm player
        RoomPlayer rp = new RoomPlayer();
        rp.setRoom(room);
        rp.setPlayer(user);
        rp.setSymbol('O');

        roomPlayerRepository.save(rp);

        // đủ 2 người → full
        if (roomPlayerRepository.countByRoom(room) == 2) {
            room.setStatus("full");
        }

        return roomRepository.save(room);
    }

    // rời phòng
    public Room leaveRoom(Long roomId, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        roomPlayerRepository.findByRoomAndPlayer(room, user)
                .ifPresent(roomPlayerRepository::delete);

        int count = roomPlayerRepository.countByRoom(room);

        if (count == 0) {
            roomRepository.delete(room);
            return null;
        } else {
            room.setStatus("waiting");
            return roomRepository.save(room);
        }
    }

    // start game
    public Room startGame(Long roomId, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room không tồn tại"));

        // ❗ check có phải host không
        if (!room.getHost().getId().equals(user.getId())) {
            throw new RuntimeException("Chỉ host mới được start game!");
        }

        // ❗ check đủ 2 người
        if (roomPlayerRepository.countByRoom(room) == 2) {
            room.setStatus("playing");
            return roomRepository.save(room);
        }

        throw new RuntimeException("Chưa đủ người để bắt đầu!");
    }

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }
}
