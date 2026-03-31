package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.RoomService;
import com.example.caro_plus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @GetMapping("/room")
    public String room(@RequestParam Long roomId,
                       @AuthenticationPrincipal CustomUserDetails customUserDetails,
                       Model model) {
        if (customUserDetails == null || customUserDetails.getUser() == null) {
            return "redirect:/";
        }

        User currentUser = userService.getPersistedUser(customUserDetails.getUser());
        if (currentUser == null) {
            return "redirect:/";
        }

        var room = roomService.getRoomById(roomId);
        if (room == null) {
            return "redirect:/home";
        }

        boolean isHost = room.getHost() != null && room.getHost().getId().equals(currentUser.getId());
        boolean isPlayer2 = room.getPlayer2() != null && room.getPlayer2().getId().equals(currentUser.getId());

        if ("playing".equals(room.getStatus()) && !isHost && !isPlayer2) {
            return "redirect:/home";
        }

        try {
            roomService.joinRoom(roomId, currentUser);
        } catch (RuntimeException exception) {
            return "redirect:/home";
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("user", currentUser);
        return "room/room";
    }
}
