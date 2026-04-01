package com.example.caro_plus.dto;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Move;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminGameDetailView {
    private Game game;
    private List<Move> moves;
}
