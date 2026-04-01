package com.example.caro_plus.repository;

import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomOrderByCreatedAtAscIdAsc(Room room);

    List<ChatMessage> findTop5ByOrderByCreatedAtDescIdDesc();

    List<ChatMessage> findTop20ByRoomOrderByCreatedAtDescIdDesc(Room room);

    boolean existsByRoom(Room room);

    boolean existsBySender(User sender);

    @Query("""
            SELECT m
            FROM ChatMessage m
            LEFT JOIN m.sender s
            LEFT JOIN m.room r
            WHERE (:query = ''
                   OR LOWER(COALESCE(s.username, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:roomId IS NULL OR r.id = :roomId)
            """)
    Page<ChatMessage> searchAdminMessages(@Param("query") String query,
                                          @Param("roomId") Long roomId,
                                          Pageable pageable);
}
