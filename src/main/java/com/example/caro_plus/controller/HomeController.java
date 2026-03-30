package com.example.caro_plus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "menu";
    }

    @GetMapping("/room")
    public String room() {
        return "room/room";
    }

    @GetMapping("/game")
    public String game() {
        return "game/game";
    }
}
