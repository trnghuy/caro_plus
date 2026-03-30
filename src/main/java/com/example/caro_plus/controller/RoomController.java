package com.example.caro_plus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RoomController {

    @GetMapping("/room")
    public String room(@RequestParam Long roomId, Model model) {
        model.addAttribute("roomId", roomId);
        return "room/room";
    }
}