package com.example.caro_plus.repository;

import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.RoomPlayer;
import com.example.caro_plus.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {
    List<RoomPlayer> findByRoom(Room room);

    boolean existsByRoomAndPlayer(Room room, User user);

    Optional<RoomPlayer> findByRoomAndPlayer(Room room, User user);

    int countByRoom(Room room);

    List<RoomPlayer> findByPlayer(User player);

    @Query("SELECT COUNT(DISTINCT rp.player.id) FROM RoomPlayer rp")
    long countDistinctPlayersInRooms();

    @Query
    ("""
    SELECT COUNT(rp) > 0 
    FROM RoomPlayer rp 
    WHERE rp.player = :user 
    AND rp.room.status IN ('waiting', 'playing')
    """)
    boolean existsActiveRoomByUser(@Param("user") User user);
}
