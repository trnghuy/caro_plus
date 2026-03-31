package com.example.caro_plus.controller;

import com.example.caro_plus.security.CustomUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class RoomController {

    @GetMapping("/room")
    public String room(@RequestParam Long roomId,
                       @AuthenticationPrincipal CustomUserDetails customUser,
                       Model model) {
        if (customUser == null) {
            return "redirect:/";
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("user", customUser.getUser());
        return "room/room";
    }

    @GetMapping("/game")
    public String game(@RequestParam Long roomId,
                       @AuthenticationPrincipal CustomUserDetails customUser,
                       Model model) {
        if (customUser == null) {
            return "redirect:/";
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("user", customUser.getUser());
        return "game/game";
    }
}
