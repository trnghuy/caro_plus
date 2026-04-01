package com.example.caro_plus.dto;

import com.example.caro_plus.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOverviewMetrics {
    private long totalUsers;
    private long adminUsers;
    private long lockedUsers;
    private long activeUsers;
    private long totalRooms;
    private long waitingRooms;
    private long fullRooms;
    private long playingRooms;
    private long totalMatches;
    private long playingMatches;
    private long finishedMatches;
    private long totalMessages;
    private double averagePoints;
    private double averageStars;
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private User topPlayer;
}
