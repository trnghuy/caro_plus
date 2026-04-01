package com.example.caro_plus.repository;

import com.example.caro_plus.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByOrderByCreatedAtDesc();

    List<Room> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(String status);

    @Query("""
            SELECT r
            FROM Room r
            LEFT JOIN r.host h
            LEFT JOIN r.player2 p
            WHERE (:query = ''
                   OR STR(r.id) LIKE CONCAT('%', :query, '%')
                   OR LOWER(COALESCE(h.username, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(COALESCE(p.username, '')) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:status = '' OR LOWER(COALESCE(r.status, '')) = LOWER(:status))
            """)
    Page<Room> searchAdminRooms(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
