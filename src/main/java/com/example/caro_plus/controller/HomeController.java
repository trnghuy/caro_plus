package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        // Lấy user từ session
        User user = (User) session.getAttribute("user");

        // Nếu đã đăng nhập, truyền đối tượng user sang View
        if (user != null) {
            model.addAttribute("user", user);
        }

        // Trả về duy nhất 1 file giao diện chính (giả sử tên file là index.html)
        return "welcome"; 
    }

   
}