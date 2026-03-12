package com.example.caro_plus.repository;

import com.example.caro_plus.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

}