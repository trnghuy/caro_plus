package com.example.caro_plus.controller;

import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails customUser) {
        if (customUser != null) {
            return "redirect:/home";
        }

        return "index";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal CustomUserDetails customUser, Model model) {
        if (customUser == null) {
            return "redirect:/";
        }

        model.addAttribute("user", userService.getPersistedUser(customUser.getUser()));
        return "home";
    }
}
