package com.example.caro_plus.dto;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.RoomPlayer;
import com.example.caro_plus.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminUserDetailView {
    private User user;
    private List<Game> recentGames;
    private List<RoomPlayer> activeRoomMemberships;
}
