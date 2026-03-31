package com.example.caro_plus.controller;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.RoomService;
import com.example.caro_plus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    // tạo phòng
    @PostMapping("/create")
    public Room createRoom(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return roomService.createRoom(resolveCurrentUser(customUserDetails));
    }

    // join phòng
    @PostMapping("/join/{roomId}")
    public Room joinRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return roomService.joinRoom(roomId, resolveCurrentUser(customUserDetails));
    }

    // leave room
    @PostMapping("/leave/{roomId}")
    public Room leaveRoom(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return roomService.leaveRoom(roomId, resolveCurrentUser(customUserDetails));
    }

    // start game
    @PostMapping("/start/{roomId}")
    public Room startGame(@PathVariable Long roomId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return roomService.startGame(roomId, resolveCurrentUser(customUserDetails));
    }

    // get room info
    @GetMapping("/{roomId:\\d+}")
    public Room getRoom(@PathVariable Long roomId) {
        return roomService.getRoomById(roomId);
    }

    private User resolveCurrentUser(CustomUserDetails customUserDetails) {
        if (customUserDetails == null || customUserDetails.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is required");
        }

        User user = userService.getPersistedUser(customUserDetails.getUser());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        return user;
    }
}
