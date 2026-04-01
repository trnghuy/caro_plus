package com.example.caro_plus.dto;

import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class AdminDashboardView {
    private AdminOverviewMetrics overview;
    private Page<User> users;
    private Page<Room> rooms;
    private Page<Game> games;
    private Page<ChatMessage> messages;
    private List<User> recentUsers;
    private List<Room> recentRooms;
    private List<Game> recentGames;
    private List<ChatMessage> recentMessages;
    private List<User> topPointUsers;
    private List<User> topStarUsers;
}
