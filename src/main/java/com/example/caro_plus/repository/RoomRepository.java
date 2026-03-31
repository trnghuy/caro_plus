package com.example.caro_plus.repository;

import com.example.caro_plus.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByOrderByCreatedAtDesc();
}
