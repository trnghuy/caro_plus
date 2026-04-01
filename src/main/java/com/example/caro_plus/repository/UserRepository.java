package com.example.caro_plus.repository;

import com.example.caro_plus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u ORDER BY u.rankScore DESC, u.win DESC, u.draw DESC, u.lose ASC, u.id ASC")
    List<User> findTopPlayers();

    @Query("""
            SELECT u
            FROM User u
            WHERE (:query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:role = '' OR u.role = :role)
            """)
    Page<User> searchAdminUsers(@Param("query") String query, @Param("role") String role, Pageable pageable);

    List<User> findTop5ByOrderByCreatedAtDesc();

    List<User> findTop5ByOrderBySupportPointsDescRankScoreDescIdAsc();

    List<User> findTop5ByOrderByRankScoreDescWinDescDrawDescLoseAscIdAsc();

    long countByRole(String role);

    long countByLockedTrue();

    Optional<User> findByUsername(String username);
}
