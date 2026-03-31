package com.example.caro_plus.service;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.GameRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Game createGame(Room room) {
        Game game = new Game();
        game.setRoom(room);
        game.setPlayerX(room.getHost());
        game.setPlayerO(room.getPlayer2());
        game.setStatus("PLAYING");
        game.setWinner(null);
        return gameRepository.save(game);
    }

    @Transactional
    public void finishGame(Room room, String winnerUsername) {
        gameRepository.findTopByRoomAndStatusOrderByIdDesc(room, "PLAYING")
                .ifPresent(game -> {
                    User winner = userRepository.findByUsername(winnerUsername)
                            .orElseThrow(() -> new RuntimeException("Winner khong ton tai"));
                    game.setWinner(winner);
                    game.setStatus("FINISHED");
                    gameRepository.save(game);
                });
    }
}
