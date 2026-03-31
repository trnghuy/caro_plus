package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService; // Đảm bảo bạn đã Autowired RoomService

    @GetMapping("/room")
    public String room(@RequestParam Long roomId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Model model) {

        User currentUser = customUserDetails.getUser();

        // --- BƯỚC QUAN TRỌNG: Ghi danh vào Database ---
        // Gọi hàm joinRoom để cập nhật player2_id nếu người này chưa có trong phòng
        roomService.joinRoom(roomId, currentUser);

        model.addAttribute("roomId", roomId);

        if (customUserDetails != null) {
            model.addAttribute("user", currentUser);
        }

        return "room/room";
    }
}