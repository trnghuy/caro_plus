package com.example.caro_plus.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "player_x")
    private User playerX;

    @ManyToOne
    @JoinColumn(name = "player_o")
    private User playerO;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    private String status;
}
