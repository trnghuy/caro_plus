package com.example.caro_plus.dto;

import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.RoomPlayer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminRoomDetailView {
    private Room room;
    private List<RoomPlayer> roomPlayers;
    private Game latestGame;
    private List<ChatMessage> recentMessages;
}
