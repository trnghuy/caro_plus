package com.example.caro_plus.repository;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findTopByRoomAndStatusOrderByIdDesc(Room room, String status);
}
