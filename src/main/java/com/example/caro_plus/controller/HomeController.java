package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user != null) {
            return "redirect:/home"; // đã login vào home
        }

        return "welcome";
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/"; // 
        }

        model.addAttribute("user", user);
        return "home";
    }
}