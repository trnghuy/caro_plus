package com.example.caro_plus.repository;

import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomOrderByCreatedAtAscIdAsc(Room room);
}
