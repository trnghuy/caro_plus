package com.example.caro_plus.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MatchHistoryItemResponse {
    private Long gameId;
    private Long roomId;
    private String opponentUsername;
    private String playerSymbol;
    private String result;
    private Date startedAt;
    private Date endedAt;
    private long totalMoves;
    private String status;
    private Integer rankScoreDelta;
    private Double supportPointsDelta;
}
