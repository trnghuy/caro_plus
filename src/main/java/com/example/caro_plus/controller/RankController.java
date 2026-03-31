package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rank")
public class RankController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<User> getRank() {
        return userRepository.findTopPlayers();
    }
}