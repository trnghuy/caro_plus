package com.example.caro_plus.controller;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {

    @Autowired
    private RoomService roomService;

    // tạo phòng
    @PostMapping("/create")
    public Room createRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();
        return roomService.createRoom(user);
    }

    // join phòng
    @PostMapping("/join/{roomId}")
    public Room joinRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();
        return roomService.joinRoom(roomId, user);
    }

    // leave room
    @PostMapping("/leave/{roomId}")
    public Room leaveRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();
        return roomService.leaveRoom(roomId, user);
    }

    // start game
    @PostMapping("/start/{roomId}")
    public Room startGame(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();
        return roomService.startGame(roomId, user);
    }

    // get room info
    @GetMapping("/{roomId:\\d+}")
    public Room getRoom(@PathVariable Long roomId) {
        return roomService.getRoomById(roomId);
    }
}