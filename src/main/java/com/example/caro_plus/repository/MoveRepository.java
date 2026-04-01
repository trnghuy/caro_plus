package com.example.caro_plus.repository;

import com.example.caro_plus.model.Move;
import com.example.caro_plus.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameOrderByMoveOrderAsc(Game game);

    List<Move> findTop2ByGameOrderByMoveOrderDesc(Game game);

    long countByGame(Game game);

    void deleteByGame(Game game);
}
