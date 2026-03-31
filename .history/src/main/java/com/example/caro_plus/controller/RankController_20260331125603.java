package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RankController {

    @Autowired
    private UserService userService;

    @GetMapping("/rank")
    public String showRankingPage(Model model) {
        List<User> topPlayers = userService.getTopPlayers();
        model.addAttribute("topPlayers", topPlayers);
        return "rank/index"; // Trả về file rank/index.html (hoặc rank.html tùy bạn đặt)
    }
}