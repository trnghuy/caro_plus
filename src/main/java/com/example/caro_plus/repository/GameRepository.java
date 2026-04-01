package com.example.caro_plus.repository;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findTopByRoomAndStatusOrderByIdDesc(Room room, String status);

    Optional<Game> findTopByRoomOrderByIdDesc(Room room);

    List<Game> findByRoomAndStatus(Room room, String status);

    List<Game> findTop5ByOrderByCreatedAtDescIdDesc();

    List<Game> findTop10ByPlayerXOrPlayerOOrderByCreatedAtDescIdDesc(User playerX, User playerO);

    @EntityGraph(attributePaths = { "room", "playerX", "playerO", "winner" })
    Page<Game> findByPlayerXOrPlayerO(User playerX, User playerO, Pageable pageable);

    long countByStatus(String status);

    long countByRoom(Room room);

    @Query("""
            SELECT COUNT(g) > 0
            FROM Game g
            WHERE g.playerX = :user OR g.playerO = :user OR g.winner = :user
            """)
    boolean existsRelatedToUser(@Param("user") User user);

    @Query("""
            SELECT g
            FROM Game g
            LEFT JOIN g.room r
            LEFT JOIN g.playerX x
            LEFT JOIN g.playerO o
            WHERE (:query = ''
                   OR STR(g.id) LIKE CONCAT('%', :query, '%')
                   OR STR(r.id) LIKE CONCAT('%', :query, '%')
                   OR LOWER(COALESCE(x.username, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(o.username, '')) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:status = '' OR LOWER(COALESCE(g.status, '')) = LOWER(:status))
            """)
    Page<Game> searchAdminGames(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
