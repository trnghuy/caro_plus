package com.example.caro_plus.controller;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    // tạo phòng
    @PostMapping("/create")
    public Room createRoom(@AuthenticationPrincipal User user) {
        return roomService.createRoom(user);
    }

    // join phòng
    @PostMapping("/join/{roomId}")
    public Room joinRoom(@PathVariable Long roomId, @AuthenticationPrincipal User user) {
        return roomService.joinRoom(roomId, user);
    }

    // start game
    @PostMapping("/start/{roomId}")
    public Room startGame(@PathVariable Long roomId, @AuthenticationPrincipal User user) {
        return roomService.startGame(roomId);
    }

    // get room info
    @GetMapping("/{roomId:\\d+}")
    public Room getRoom(@PathVariable Long roomId) {
        return roomService.getRoomById(roomId);
    }
}