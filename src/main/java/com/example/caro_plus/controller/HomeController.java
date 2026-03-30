package com.example.caro_plus.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.caro_plus.security.CustomUserDetails;

@Controller
public class HomeController {

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

        model.addAttribute("user", customUser.getUser());
        return "home";
    }
}