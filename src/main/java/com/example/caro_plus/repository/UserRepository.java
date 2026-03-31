package com.example.caro_plus.repository;

import com.example.caro_plus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u ORDER BY u.rating DESC")
    List<User> findTopPlayers();

    Optional<User> findByUsername(String username);
}
