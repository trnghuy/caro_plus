package com.example.caro_plus.repository;

import com.example.caro_plus.model.Move;
import com.example.caro_plus.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameOrderByMoveOrderAsc(Game game);

    List<Move> findTop2ByGameOrderByMoveOrderDesc(Game game);

    long countByGame(Game game);

    @Query("""
            SELECT m.game.id, COUNT(m)
            FROM Move m
            WHERE m.game.id IN :gameIds
            GROUP BY m.game.id
            """)
    List<Object[]> countMovesByGameIds(@Param("gameIds") List<Long> gameIds);

    void deleteByGame(Game game);
}
