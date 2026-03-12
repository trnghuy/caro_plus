package com.example.caro_plus.controller;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.service.RoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    // create room
    @PostMapping("/create")
    public Room createRoom(@RequestBody User user){
        return roomService.createRoom(user);
    }

    // join room
    @PostMapping("/join/{roomId}")
    public Room joinRoom(@PathVariable Long roomId){
        return roomService.joinRoom(roomId);
    }

    // start game
    @PostMapping("/start/{roomId}")
    public Room startGame(@PathVariable Long roomId){
        return roomService.startGame(roomId);
    }
}
